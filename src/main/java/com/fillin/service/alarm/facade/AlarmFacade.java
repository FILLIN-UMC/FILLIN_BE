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
                Member target = event.getTarget();
                NotificationSetting settings = notiSetRepository.findByMember(target);

                if (settings == null || !isAlarmEnabled(settings, event.getType())) {
                        return;
                }

                Alarm alarm = Alarm.builder()
                                .member(target)
                                .alarmType(event.getType())
                                .message(event.getMessage())
                                .referId(event.getReferId())
                                .isRead(false)
                                .build();

                alarmRepository.save(alarm);

                fcmService.send(
                                target.getFcmToken(),
                                "FillIn 알림",
                                event.getMessage(),
                                Map.of(
                                                "alarmId", alarm.getId().toString(),
                                                "type", event.getType().name(),
                                                "referId", String.valueOf(event.getReferId())));
        }

        private boolean isAlarmEnabled(NotificationSetting settings, AlarmType type) {
                return switch (type) {
                        case REPORT -> settings.getIsReportAlarm();
                        case LIKE, LEVEL_UP, EXPIRATION -> settings.getIsFeedbackAlarm();
                        case NOTICE -> settings.getIsServiceAlarm();
                };
        }
}
