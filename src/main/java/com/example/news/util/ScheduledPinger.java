package com.example.news.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ScheduledPinger {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledPinger.class);

    private final RestTemplate restTemplate;

    // Read server port (default to 8080)
    @Value("${server.port:8080}")
    private int serverPort;

    public ScheduledPinger(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Ping every 13 minutes (13 * 60 * 1000 ms)
    @Scheduled(fixedRate = 13 * 60 * 1000)
    public void pingSelf() {
        try {
            String url = "http://localhost:" + serverPort + "/api/ping";
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            logger.info("Ping to {} returned status {} and body: {}", url, resp.getStatusCode().value(), resp.getBody());
        } catch (Exception e) {
            logger.error("Scheduled ping failed", e);
        }
    }
}
