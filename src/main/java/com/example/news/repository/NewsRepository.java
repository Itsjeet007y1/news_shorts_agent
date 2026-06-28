package com.example.news.repository;

import com.example.news.model.News;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class NewsRepository {

  private static final String COUNTERS_COLLECTION = "_metadata";
  private static final String NEWS_COUNTER_DOCUMENT = "news_id_counter";
  private static final String NEXT_ID_FIELD = "nextId";
  private static final DateTimeFormatter PUBLISHED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id");
  private static final Set<String> SERVER_SIDE_SORTABLE_FIELDS = Set.of(
    "id", "sourceId", "sourceName", "author", "title", "description",
    "url", "imageUrl", "publishedAt", "content", "category", "language"
  );

  private final Firestore firestore;

  public NewsRepository(Firestore firestore) {
    this.firestore = firestore;
  }

  public List<News> findAll() {
    return execute(() -> toNewsList(applySort(newsCollection(), DEFAULT_SORT).get().get()));
  }

  public List<News> findAll(Sort sort) {
    Sort effectiveSort = normalizeSort(sort);
    if (canUseServerSideSort(effectiveSort)) {
      return execute(() -> toNewsList(applySort(newsCollection(), effectiveSort).get().get()));
    }
    return applySort(findAll(), effectiveSort);
  }

  public Page<News> findAll(Pageable pageable) {
    Sort effectiveSort = normalizeSort(pageable.getSort());
    if (canUseServerSideSort(effectiveSort)) {
      return execute(() -> {
        Query query = applySort(newsCollection(), effectiveSort)
          .offset(Math.toIntExact(pageable.getOffset()))
          .limit(pageable.getPageSize());
        List<News> pageContent = toNewsList(query.get().get());
        long total = newsCollection().get().get().size();
        return new PageImpl<>(pageContent, pageable, total);
      });
    }

    List<News> allNews = applySort(findAll(), effectiveSort);
    int start = Math.toIntExact(pageable.getOffset());
    if (start >= allNews.size()) {
      return new PageImpl<>(List.of(), pageable, allNews.size());
    }
    int end = Math.min(start + pageable.getPageSize(), allNews.size());
    return new PageImpl<>(allNews.subList(start, end), pageable, allNews.size());
  }

  public Optional<News> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return execute(() -> {
      DocumentSnapshot snapshot = newsDocument(id).get().get();
      return snapshot.exists() ? Optional.of(toNews(snapshot)) : Optional.empty();
    });
  }

  public List<News> findByLanguage(String language) {
    if (language == null) {
      return List.of();
    }
    return execute(() -> {
      QuerySnapshot snapshot = newsCollection()
        .whereEqualTo("language", language)
        .orderBy("id", Query.Direction.ASCENDING)
        .get()
        .get();
      return toNewsList(snapshot);
    });
  }

  public Optional<News> findByIdAndLanguage(Long id, String language) {
    return findById(id)
      .filter(news -> language == null || language.equals(news.getLanguage()));
  }

  public News save(News news) {
    if (news == null) {
      throw new IllegalArgumentException("News must not be null");
    }

    Long id = news.getId();
    if (id == null) {
      id = nextId();
      news.setId(id);
    }

    Long finalId = id;
    execute(() -> {
      newsDocument(finalId).set(toDocument(news)).get();
      return null;
    });
    return news;
  }

  public List<News> saveAll(List<News> newsList) {
    if (newsList == null) {
      throw new IllegalArgumentException("News list must not be null");
    }
    List<News> savedNews = new ArrayList<>(newsList.size());
    for (News news : newsList) {
      savedNews.add(save(news));
    }
    return savedNews;
  }

  public void deleteById(Long id) {
    if (id == null) {
      return;
    }
    execute(() -> {
      newsDocument(id).delete().get();
      return null;
    });
  }

  private CollectionReference newsCollection() {
    return firestore.collection(News.COLLECTION_NAME);
  }

  private DocumentReference newsDocument(Long id) {
    return newsCollection().document(String.valueOf(id));
  }

  private Long nextId() {
    return execute(() -> firestore.runTransaction(transaction -> {
      DocumentReference counterDocument = firestore.collection(COUNTERS_COLLECTION).document(NEWS_COUNTER_DOCUMENT);
      DocumentSnapshot snapshot = transaction.get(counterDocument).get();
      Long currentId = snapshot.exists() ? snapshot.getLong(NEXT_ID_FIELD) : null;
      long nextId = (currentId == null ? 0L : currentId) + 1L;
      Map<String, Object> counterData = new HashMap<>();
      counterData.put(NEXT_ID_FIELD, nextId);
      transaction.set(counterDocument, counterData);
      return nextId;
    }).get());
  }

  private List<News> toNewsList(QuerySnapshot querySnapshot) {
    return querySnapshot.getDocuments().stream()
      .map(this::toNews)
      .collect(Collectors.toList());
  }

  private News toNews(DocumentSnapshot documentSnapshot) {
    News news = new News();
    news.setId(resolveId(documentSnapshot));
    news.setSourceId(documentSnapshot.getString("sourceId"));
    news.setSourceName(documentSnapshot.getString("sourceName"));
    news.setAuthor(documentSnapshot.getString("author"));
    news.setTitle(documentSnapshot.getString("title"));
    news.setDescription(documentSnapshot.getString("description"));
    news.setUrl(documentSnapshot.getString("url"));
    news.setImageUrl(documentSnapshot.getString("imageUrl"));
    news.setPublishedAt(readPublishedAt(documentSnapshot.get("publishedAt")));
    news.setContent(documentSnapshot.getString("content"));
    news.setCategory(documentSnapshot.getString("category"));
    news.setLanguage(documentSnapshot.getString("language"));
    return news;
  }

  private Long resolveId(DocumentSnapshot documentSnapshot) {
    Long explicitId = documentSnapshot.getLong("id");
    if (explicitId != null) {
      return explicitId;
    }
    try {
      return Long.parseLong(documentSnapshot.getId());
    } catch (NumberFormatException ex) {
      throw new IllegalStateException("Invalid Firestore document id for News: " + documentSnapshot.getId(), ex);
    }
  }

  private Map<String, Object> toDocument(News news) {
    Map<String, Object> document = new HashMap<>();
    document.put("id", news.getId());
    document.put("sourceId", news.getSourceId());
    document.put("sourceName", news.getSourceName());
    document.put("author", news.getAuthor());
    document.put("title", news.getTitle());
    document.put("description", news.getDescription());
    document.put("url", news.getUrl());
    document.put("imageUrl", news.getImageUrl());
    document.put("publishedAt", writePublishedAt(news.getPublishedAt()));
    document.put("content", news.getContent());
    document.put("category", news.getCategory());
    document.put("language", news.getLanguage());
    return document;
  }

  private String writePublishedAt(LocalDateTime publishedAt) {
    return publishedAt == null ? null : publishedAt.format(PUBLISHED_AT_FORMATTER);
  }

  private LocalDateTime readPublishedAt(Object publishedAtValue) {
    if (publishedAtValue == null) {
      return null;
    }
    if (publishedAtValue instanceof String value) {
      if (value.isBlank()) {
        return null;
      }
      try {
        return LocalDateTime.parse(value, PUBLISHED_AT_FORMATTER);
      } catch (DateTimeParseException ex) {
        throw new IllegalStateException("Invalid publishedAt value in Firestore: " + value, ex);
      }
    }
    if (publishedAtValue instanceof Timestamp value) {
      return LocalDateTime.ofInstant(value.toDate().toInstant(), ZoneOffset.UTC);
    }
    throw new IllegalStateException("Unsupported publishedAt type from Firestore: " + publishedAtValue.getClass().getName());
  }

  private List<News> applySort(List<News> newsList, Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return newsList;
    }
    Comparator<News> comparator = null;
    for (Sort.Order order : sort) {
      Comparator<News> nextComparator = Comparator.comparing(
        news -> extractComparableValue(news, order.getProperty()),
        Comparator.nullsLast(Comparator.naturalOrder())
      );
      if (order.isDescending()) {
        nextComparator = nextComparator.reversed();
      }
      comparator = comparator == null ? nextComparator : comparator.thenComparing(nextComparator);
    }
    return newsList.stream().sorted(comparator).collect(Collectors.toList());
  }

  private Query applySort(Query query, Sort sort) {
    Sort effectiveSort = normalizeSort(sort);
    Query sortedQuery = query;
    for (Sort.Order order : effectiveSort) {
      sortedQuery = sortedQuery.orderBy(
        order.getProperty(),
        order.isDescending() ? Query.Direction.DESCENDING : Query.Direction.ASCENDING
      );
    }
    return sortedQuery;
  }

  private Sort normalizeSort(Sort sort) {
    return sort == null || sort.isUnsorted() ? DEFAULT_SORT : sort;
  }

  private boolean canUseServerSideSort(Sort sort) {
    Sort effectiveSort = normalizeSort(sort);
    int orderCount = 0;
    for (Sort.Order order : effectiveSort) {
      orderCount++;
      if (!SERVER_SIDE_SORTABLE_FIELDS.contains(order.getProperty())) {
        return false;
      }
    }
    return orderCount <= 1;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Comparable extractComparableValue(News news, String property) {
    return switch (property) {
      case "id" -> news.getId();
      case "sourceId" -> news.getSourceId();
      case "sourceName" -> news.getSourceName();
      case "author" -> news.getAuthor();
      case "title" -> news.getTitle();
      case "description" -> news.getDescription();
      case "url" -> news.getUrl();
      case "imageUrl" -> news.getImageUrl();
      case "publishedAt" -> news.getPublishedAt();
      case "content" -> news.getContent();
      case "category" -> news.getCategory();
      case "language" -> news.getLanguage();
      default -> throw new IllegalArgumentException("Unsupported sort property: " + property);
    };
  }

  private <T> T execute(FirestoreSupplier<T> supplier) {
    try {
      return supplier.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Firestore operation was interrupted", ex);
    } catch (Exception ex) {
      throw new IllegalStateException("Firestore operation failed", ex);
    }
  }

  @FunctionalInterface
  private interface FirestoreSupplier<T> {
    T get() throws Exception;
  }
}
