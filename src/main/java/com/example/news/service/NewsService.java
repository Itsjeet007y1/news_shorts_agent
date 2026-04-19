package com.example.news.service;

import com.example.news.model.News;
import com.example.news.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class NewsService {
	@Autowired
	private NewsRepository newsRepository;

	public List<News> getAllNews(String language) {
		if (language != null && !language.isEmpty()) {
			return newsRepository.findByLanguage(language);
		}
		return newsRepository.findAll();
	}

	public Optional<News> getNewsById(Long id, String language) {
		if (language != null && !language.isEmpty()) {
			return newsRepository.findByIdAndLanguage(id, language);
		}
		return newsRepository.findById(id);
	}

	public News saveNews(News news) {
		return newsRepository.save(news);
	}

	public List<News> saveAllNews(List<News> newsList) {
		return newsRepository.saveAll(newsList);
	}

	public News updateNews(Long id, News updatedNews) {
		return newsRepository.findById(id).map(news -> {
			String existingImageUrl = news.getImageUrl();
			String incomingImageUrl = updatedNews.getImageUrl();
			boolean shouldUpdateImage = false;
			if (incomingImageUrl != null && !incomingImageUrl.isEmpty() && !incomingImageUrl.equals(existingImageUrl)) {
				// Only update if incoming image URL is different from existing
				shouldUpdateImage = true;
			}
			if (shouldUpdateImage) {
				news.setImageUrl(incomingImageUrl);
			}
			news.setSourceId(updatedNews.getSourceId());
			news.setSourceName(updatedNews.getSourceName());
			news.setAuthor(updatedNews.getAuthor());
			news.setTitle(updatedNews.getTitle());
			news.setDescription(updatedNews.getDescription());
			news.setUrl(updatedNews.getUrl());
			news.setPublishedAt(updatedNews.getPublishedAt());
			news.setContent(updatedNews.getContent());
			news.setCategory(updatedNews.getCategory());
			news.setLanguage(updatedNews.getLanguage());
			return newsRepository.save(news);
		}).orElse(null);
	}

	// ...existing code...

	public void deleteNews(Long id) {
		newsRepository.deleteById(id);
	}
}
