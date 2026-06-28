package com.example.news.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(FirestoreBeanConfig.class)
public class FirestoreConfig {
}
