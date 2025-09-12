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

	public News updateNews(Long id, News updatedNews) {
		return newsRepository.findById(id).map(news -> {
			String existingImage = news.getBase64Image();
			String incomingImage = updatedNews.getBase64Image();
			boolean shouldUpdateImage = false;
			if (incomingImage != null && !incomingImage.isEmpty() && !isEmptyOrDefaultImage(incomingImage)) {
				// Only update if incoming image is different from existing
				if (existingImage == null || !existingImage.equals(incomingImage)) {
					shouldUpdateImage = true;
				}
			}
			if (shouldUpdateImage) {
				news.setBase64Image(incomingImage);
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

	private boolean isEmptyOrDefaultImage(String image) {
		// Check for empty string or string of all 'A's (base64 for zeros)
		if (image == null || image.isEmpty()) return true;
		// Optionally, check for a known default base64 string if needed
		return false;
	}

	public void deleteNews(Long id) {
		newsRepository.deleteById(id);
	}
}
