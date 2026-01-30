package com.fillin.service.alarm.facade;

import com.fillin.domain.Alarm;
import com.fillin.domain.Member;
import com.fillin.domain.NotificationSetting;
import com.fillin.domain.enums.AlarmType;
import com.fillin.global.event.AlarmEvent;
import com.fillin.infrastructure.fcm.FcmService;
import com.fillin.repository.NotiSetRepository;
import com.fillin.repository.alarm.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlarmFacade {

        private final AlarmRepository alarmRepository;
        private final FcmService fcmService;
        private final NotiSetRepository notiSetRepository;

        @Transactional
        @EventListener
        public void handleAlarmEvent(AlarmEvent event) {
            Member target = event.getReceiver();
            AlarmType type = event.getAlarmType();

            NotificationSetting settings = notiSetRepository
                    .findByMember(target);

            if (settings == null || !isAlarmEnabled(settings, type)) {
                return;
            }

            String message = type.buildMessage(event.getContext());

            Alarm alarm = Alarm.builder()
                    .member(target)
                    .alarmType(type)
                    .message(message)
                    .referId(event.getReferId())
                    .isRead(false)
                    .build();

            alarmRepository.save(alarm);

            if (target.getFcmToken() == null) return;

            Map<String, String> data = new HashMap<>();
            data.put("alarmId", alarm.getId().toString());
            data.put("type", type.name());
            if (event.getReferId() != null) {
                data.put("referId", event.getReferId().toString());
            }

            fcmService.send(
                    target.getFcmToken(),
                    "FillIn 알림",
                    message,
                    data
            );
        }


        private boolean isAlarmEnabled(NotificationSetting settings, AlarmType type) {
                    return switch (type) {
                            case REPORT -> settings.getIsReportAlarm();
                            case LIKE, LEVEL_UP, EXPIRATION -> settings.getIsFeedbackAlarm();
                            case NOTICE -> settings.getIsServiceAlarm();
                    };
            }
}
