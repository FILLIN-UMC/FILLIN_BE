package com.fillin.service.alarm;

import com.fillin.global.event.AlarmSendEvent;
import com.fillin.infrastructure.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AlarmSendListener {

    private final FcmService fcmService;

    @Async
    @EventListener
    public void handle(AlarmSendEvent event) {

        if (event.getFcmToken() == null) return;

        Map<String, String> data = new HashMap<>();
        data.put("alarmId", event.getAlarmId().toString());
        data.put("type", event.getType().name());
        if (event.getReferId() != null) {
            data.put("referId", event.getReferId().toString());
        }

        fcmService.send(
                event.getFcmToken(),
                "FillIn 알림",
                event.getMessage(),
                data
        );
    }
}

