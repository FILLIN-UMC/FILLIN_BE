package com.fillin.service.alarm;

import com.fillin.domain.Alarm;
import com.fillin.domain.enums.AlarmType;
import com.fillin.global.event.AlarmEvent;
import com.fillin.repository.alarm.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmEventListener {

    private final AlarmRepository alarmRepository;

    @EventListener
    public void handle(AlarmEvent event) {

        AlarmType type = event.getAlarmType();

        Alarm alarm = Alarm.builder()
                .member(event.getReceiver())
                .alarmType(type)
                .message(type.buildMessage(event.getContext()))
                .referId(event.getReferId())
                .isRead(false)
                .build();

        alarmRepository.save(alarm);
    }
}

