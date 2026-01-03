package com.fillin.domain;

import com.fillin.domain.common.BaseTimeEntity;
import com.fillin.domain.Member;
import com.fillin.domain.enums.AlarmType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    private String message;

    private Long referId; // 이동할 타겟 ID (제보 ID 등), 필요 시 연관관계 매핑으로 변경 가능

    private Boolean isRead;
}
