package com.example.news.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
public class  News {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "sourceId")
	private String sourceId;
	@Column(name = "sourceName")
	private String sourceName;
	private String author;
	private String title;
	private String description;
	private String url;
	@Lob
	@Column(name = "base64Image", columnDefinition = "LONGTEXT")
	private String base64Image;
	@Column(name = "publishedAt")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime publishedAt;
	private String content;
	private String category;
	private String language;

	public News() {}

	public News(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getSourceId() { return sourceId; }
	public void setSourceId(String sourceId) { this.sourceId = sourceId; }
	public String getSourceName() { return sourceName; }
	public void setSourceName(String sourceName) { this.sourceName = sourceName; }
	public String getAuthor() { return author; }
	public void setAuthor(String author) { this.author = author; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getUrl() { return url; }
	public void setUrl(String url) { this.url = url; }
	public String getBase64Image() { return base64Image; }
	public void setBase64Image(String base64Image) { this.base64Image = base64Image; }
	public LocalDateTime getPublishedAt() { return publishedAt; }
	public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }
	public String getCategory() { return category; }
	public void setCategory(String category) { this.category = category; }
	public String getLanguage() { return language; }
	public void setLanguage(String language) { this.language = language; }
}
