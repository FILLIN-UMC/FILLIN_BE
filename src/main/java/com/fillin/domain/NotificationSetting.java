package com.fillin.domain;

import com.fillin.dto.mypage.request.ProfileRequestDto;
import com.fillin.dto.notiSet.request.NotificationUpdateRequestDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting { // 알림 설정

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder.Default
    private Boolean isReportAlarm = true;       // 제보 관련 알림 수신 여부

    @Builder.Default
    private Boolean isFeedbackAlarm = true;   // 피드백 관련 알림 수신 여부

    @Builder.Default
    private Boolean isServiceAlarm = true;    // 서비스 관련 알림 수신 여부

    public NotificationSetting(Long id, Member member, Boolean isReportAlarm, Boolean isFeedbackAlarm, Boolean isServiceAlarm) {
        this.id = id;
        this.member = member;
        this.isReportAlarm = isReportAlarm;
        this.isFeedbackAlarm = isFeedbackAlarm;
        this.isServiceAlarm = isServiceAlarm;
    }

    public void updateAlarmSetting(NotificationUpdateRequestDto dto) {
        if (dto.getFeedbackAlarm() != null) {
            this.isFeedbackAlarm = dto.getFeedbackAlarm();
        }
        if (dto.getServiceAlarm() != null) {
            this.isServiceAlarm = dto.getServiceAlarm();
        }
        if (dto.getReportAlarm() != null) {
            this.isReportAlarm = dto.getReportAlarm();
        }
    }
}
