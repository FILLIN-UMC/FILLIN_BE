package com.fillin.dto.notiSet.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationUpdateResponseDto {
    private Long memberId;
    private Boolean reportAlarm;      // 제보 관련 알림 수신 여부
    private Boolean feedbackAlarm;   // 피드백 관련 알림 수신 여부
    private Boolean serviceAlarm;
}
