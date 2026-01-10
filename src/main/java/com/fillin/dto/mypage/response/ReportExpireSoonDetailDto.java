package com.fillin.dto.mypage.response;

import com.fillin.domain.enums.Achievement;
import com.fillin.domain.enums.ReportCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExpireSoonDetailDto {
    private Long memberId;
    private Achievement achievement;
    private String profileImageUrl;
    private Long reportId;
    private ReportCategory reportCategory;
    private String reportImageUrl;
    private String title;
    private Double latitude;
    private Double longitude;
    private String address;
    private LocalDateTime expireTime;
    private int viewCount;
    private LocalDateTime createAt;
    private int doneCount;
    private int nowCount;
}
