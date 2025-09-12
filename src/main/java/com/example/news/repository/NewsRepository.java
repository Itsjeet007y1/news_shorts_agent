package com.example.news.repository;

import com.example.news.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByLanguage(String language);
    Optional<News> findByIdAndLanguage(Long id, String language);
}
