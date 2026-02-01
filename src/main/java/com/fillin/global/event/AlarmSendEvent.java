package com.fillin.global.event;

import com.fillin.domain.enums.AlarmType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmSendEvent {
    private String fcmToken;
    private Long alarmId;
    private AlarmType type;
    private String message;
    private Long referId;
}

