package com.fillin.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    // 1. 서버에 올릴 때 사용할 경로 (도커 내부 경로)
    private static final String SERVER_KEY_PATH = "/app/firebase-key.json";

    // 2. 로컬 개발할 때 사용할 경로 (resources 폴더 안)
    private static final String LOCAL_KEY_PATH = "firebase/firebase-service-account.json";

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = null;
            File serverFile = new File(SERVER_KEY_PATH);

            // 서버용 파일이 실제로 존재하는지 확인
            if (serverFile.exists()) {
                log.info("[Firebase] Loading key from SERVER path: {}", SERVER_KEY_PATH);
                serviceAccount = new FileInputStream(serverFile);
            } else {
                // 서버 없으면 로컬에서 찾기
                log.info("[Firebase] Server key not found. Loading from CLASSPATH: {}", LOCAL_KEY_PATH);
                ClassPathResource resource = new ClassPathResource(LOCAL_KEY_PATH);
                if (resource.exists()) {
                    serviceAccount = resource.getInputStream();
                }
            }

            if (serviceAccount == null) {
                log.warn("[Firebase] Key not found anywhere. Firebase disabled.");
                return;
            }

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
