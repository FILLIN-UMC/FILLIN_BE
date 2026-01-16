package com.fillin.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    private static final String FIREBASE_KEY_PATH =
            "firebase/firebase-service-account.json";

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource =
                    new ClassPathResource(FIREBASE_KEY_PATH);

            if (!resource.exists()) {
                log.warn("[Firebase] Service account not found. Firebase disabled.");
                return;
            }

            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("[Firebase] Initialized successfully");
            }

        } catch (Exception e) {
            log.error("[Firebase] Initialization failed. Firebase disabled.", e);
        }
    }
}
