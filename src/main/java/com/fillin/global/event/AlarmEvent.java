package com.fillin.global.event;

import com.fillin.domain.AlarmContext;
import com.fillin.domain.Member;
import com.fillin.domain.enums.AlarmType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmEvent {
    private Member receiver;
    private AlarmType alarmType;
    private AlarmContext context;
    private Long referId;
}

