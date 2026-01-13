package com.example.news;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@SpringBootApplication
@EnableScheduling
public class NewsShortsAgentApplication {
	public static void main(String[] args) {
		SpringApplication.run(NewsShortsAgentApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		// Configure timeouts to avoid a stalled scheduled task
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3000); // 3 seconds
		factory.setReadTimeout(5000); // 5 seconds
		return new RestTemplate(factory);
	}
}
