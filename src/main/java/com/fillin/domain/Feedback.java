package com.fillin.domain;

import com.fillin.domain.common.BaseTimeEntity;
import com.fillin.domain.enums.FeedbackType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report; //피드백한 제보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; //피드백 작성자

    private FeedbackType type;

    public Feedback(int id, Report report, Member member, FeedbackType type) {
        this.id = id;
        this.report = report;
        this.member = member;
        this.type = type;
    }

    public void updateFeedbackType(FeedbackType type) {
        this.type = type;
    }
}
