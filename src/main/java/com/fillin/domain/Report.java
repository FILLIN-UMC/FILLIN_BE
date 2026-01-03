package com.fillin.domain;

import com.fillin.domain.common.BaseTimeEntity;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.domain.enums.ReportStatus;
import com.fillin.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private ReportCategory category;

    private String keyword; // AI 분석 키워드 등

    private String title;

    @Column(columnDefinition = "TEXT")
    @Lob
    private String content;

    // 위도
    @Column(columnDefinition = "DECIMAL(10, 8)")
    private Double latitude;

    // 경도
    @Column(columnDefinition = "DECIMAL(11, 8)")
    private Double longitude;

    private String address;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(columnDefinition = "integer default 0")
    private int viewCount;

    @Column(columnDefinition = "integer default 0")
    private int likeCount;

    private LocalDateTime expiresAt; // 만료 시간

    public void addLikeCount() {
        this.likeCount++;
    }

    public void removeLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
