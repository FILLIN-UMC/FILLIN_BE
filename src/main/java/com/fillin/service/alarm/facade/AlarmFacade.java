package com.fillin.service.alarm.facade;

import com.fillin.domain.Alarm;
import com.fillin.domain.Member;
import com.fillin.domain.NotificationSetting;
import com.fillin.domain.enums.AlarmType;
import com.fillin.global.event.AlarmEvent;
import com.fillin.global.event.AlarmSendEvent;
import com.fillin.repository.NotiSetRepository;
import com.fillin.repository.alarm.AlarmRepository;
import com.fillin.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class AlarmFacade {

    private final AlarmRepository alarmRepository;
    private final NotiSetRepository notiSetRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleAlarmEvent(AlarmEvent event) {

        Member target = memberRepository.findById(event.getReceiverId())
                .orElseThrow();

        AlarmType type = event.getAlarmType();

        NotificationSetting settings =
                notiSetRepository.findByMember(target);

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

        // 👉 전송 이벤트 발행
        eventPublisher.publishEvent(
                new AlarmSendEvent(
                        target.getFcmToken(),
                        alarm.getId(),
                        type,
                        message,
                        event.getReferId()
                )
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
