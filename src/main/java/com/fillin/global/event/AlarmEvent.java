package com.fillin.global.event;

import com.fillin.domain.Member;
import com.fillin.domain.enums.AlarmType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AlarmEvent {
    private final Member target;
    private final AlarmType type;
    private final String message;
    private final Long referId;
}
