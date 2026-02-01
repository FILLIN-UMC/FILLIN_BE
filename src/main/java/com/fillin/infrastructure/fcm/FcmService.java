package com.fillin.infrastructure.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class FcmService {

    @Async("alarmExecutor")
    public void send(
            String token,
            String title,
            String body,
            Map<String, String> data) {

        if (FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM] Firebase not initialized. Skip send.");
            return;
        }

        if (token == null || token.isBlank()) {
            return;
        }

        Message message = Message.builder()
                .setToken(token)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                .putAllData(data)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("FCM 전송 실패 (Token: {}): {}", token, e.getMessage());
        }
    }
}
