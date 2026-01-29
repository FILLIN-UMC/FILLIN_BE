package com.fillin.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlarmContext {

    private String actorName;   // 좋아요 / 피드백 남긴 사람
    private String feedback;    // REPORT
    private Integer count;      // EXPIRATION, LEVEL_UP
    private String badgeName;   // LEVEL_UP
    private String message;     // NOTICE
}
