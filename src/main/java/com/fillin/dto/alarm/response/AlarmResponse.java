package com.fillin.dto.alarm.response;

import com.fillin.domain.Alarm;
import com.fillin.domain.enums.AlarmType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AlarmResponse {

    private Long alarmId;
    private AlarmType alarmType;
    private String message;
    private Boolean read;
    private Long referId;
    private LocalDateTime createdAt;

    public static AlarmResponse from(Alarm alarm) {
        return new AlarmResponse(
                alarm.getId(),
                alarm.getAlarmType(),
                alarm.getMessage(),
                alarm.getIsRead(),
                alarm.getReferId(),
                alarm.getCreatedAt()
        );
    }
}
