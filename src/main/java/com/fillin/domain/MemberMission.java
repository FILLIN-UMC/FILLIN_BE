package com.fillin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMission { // 유저별 미션 진행도

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    private int progressCount;

    private Boolean isComplete;
    private LocalDateTime completedAt;

    public void addProgress() {
        this.progressCount++;
    }

    public void complete() {
        this.isComplete = true;
        this.completedAt = LocalDateTime.now();
    }
}
