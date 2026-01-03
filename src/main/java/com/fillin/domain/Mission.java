package com.fillin.domain;

import com.fillin.domain.common.BaseTimeEntity;
import com.fillin.domain.Member;
import com.fillin.domain.enums.ReportCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseTimeEntity{ // 미션 마스터 정보

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_id")
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ReportCategory category; // 관련 카테고리 (위험, 불편 등)

    private int targetCount; // 목표 횟수
    private int exp;         // 보상 경험치
}
