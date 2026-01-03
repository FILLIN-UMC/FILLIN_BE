package com.fillin.domain;

import com.fillin.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// SQL 예약어 'LIKE' 충돌 방지를 위해 테이블명을 'likes'로 지정
@Table(
        name = "likes",
        uniqueConstraints = {
                // 한 유저가 같은 제보에 중복 좋아요 방지
                @UniqueConstraint(
                        name = "uk_member_report_like",
                        columnNames = {"member_id", "report_id"}
                )
        }
)
public class Like extends BaseTimeEntity { // created_at 자동 관리

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Builder
    public Like(Member member, Report report) {
        this.member = member;
        this.report = report;
    }

}
