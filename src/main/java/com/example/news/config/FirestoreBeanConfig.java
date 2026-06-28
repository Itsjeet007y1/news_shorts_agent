package com.example.news.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirestoreBeanConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreBeanConfig.class);
    private static final String DEFAULT_DATABASE_ID = "(default)";

    @Value("${firestore.project-id:}")
    private String configuredProjectId;

    @Value("${firestore.database-id:(default)}")
    private String databaseId;

    private Firestore firestore;

    @Bean
    public Firestore firestore() throws IOException {
        String resolvedProjectId = resolveProjectId();
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(resolvedProjectId)
                .setDatabaseId(resolveDatabaseId())
                .build();

        this.firestore = options.getService();

        logger.info("Initialized Firestore client with projectId='{}', databaseId='{}' using Application Default Credentials.",
                resolvedProjectId, resolveDatabaseId());

        return this.firestore;
    }

    private String resolveProjectId() {
        if (configuredProjectId != null && !configuredProjectId.isBlank()) {
            logger.info("Using Firestore project ID from 'firestore.project-id' property: {}", configuredProjectId);
            return configuredProjectId;
        }

        String defaultProjectId = ServiceOptions.getDefaultProjectId();
        if (defaultProjectId != null && !defaultProjectId.isBlank()) {
            logger.info("Using Firestore project ID discovered from Application Default Credentials/environment: {}", defaultProjectId);
            return defaultProjectId;
        }

        throw new IllegalStateException(
                "Unable to resolve Google Cloud project ID for Firestore. " +
                "Set 'firestore.project-id' or configure Application Default Credentials with an associated project."
        );
    }

    private String resolveDatabaseId() {
        if (databaseId == null || databaseId.isBlank()) {
            return DEFAULT_DATABASE_ID;
        }
        return databaseId;
    }

    @PreDestroy
    public void closeFirestore() throws Exception {
        if (this.firestore != null) {
            logger.info("Closing Firestore client.");
            this.firestore.close();
        }
    }
}

