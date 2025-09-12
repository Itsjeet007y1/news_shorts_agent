package com.example.news.controller;

import com.example.news.model.News;
import com.example.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/news")
public class NewsController {
	@Autowired
	private NewsService newsService;

	@GetMapping
	public List<News> getAllNews(@RequestParam(value = "language", required = false) String language) {
		return newsService.getAllNews(language);
	}

	@GetMapping("/{id}")
	public ResponseEntity<News> getNewsById(@PathVariable Long id, @RequestParam(value = "language", required = false) String language) {
		return newsService.getNewsById(id, language)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping(consumes = {"multipart/form-data"})
	public ResponseEntity<?> createNews(
		@ModelAttribute News news,
		@RequestPart(value = "image", required = false) MultipartFile image
	) {
		if (news == null || news.getTitle() == null || news.getContent() == null) {
			return ResponseEntity.badRequest().body("Missing required news fields in request.");
		}
		try {
			if (image != null && !image.isEmpty()) {
				String base64 = Base64.getEncoder().encodeToString(image.getBytes());
				news.setBase64Image(base64);
			}
		} catch (java.io.IOException e) {
			return ResponseEntity.status(500).body("Error processing image: " + e.getMessage());
		}
		return ResponseEntity.ok(newsService.saveNews(news));
	}

	@PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
	public ResponseEntity<?> updateNews(
		@PathVariable Long id,
		@ModelAttribute News news,
		@RequestPart(value = "image", required = false) MultipartFile image
	) {
		if (news == null || news.getTitle() == null || news.getContent() == null) {
			return ResponseEntity.badRequest().body("Missing required news fields in request.");
		}
		try {
			if (image != null && !image.isEmpty()) {
				String base64 = Base64.getEncoder().encodeToString(image.getBytes());
				news.setBase64Image(base64);
			}
		} catch (java.io.IOException e) {
			return ResponseEntity.status(500).body("Error processing image: " + e.getMessage());
		}
		News updated = newsService.updateNews(id, news);
		if (updated == null) return ResponseEntity.notFound().build();
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
		newsService.deleteNews(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/testConnectivity")
	public ResponseEntity<String> testConnectivity() {
		return ResponseEntity.ok("Connection successful");
	}
}
