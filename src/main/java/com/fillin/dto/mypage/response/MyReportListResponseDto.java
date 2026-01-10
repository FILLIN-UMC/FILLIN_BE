package com.fillin.dto.mypage.response;

import com.fillin.domain.enums.ReportCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyReportListResponseDto {
    private Long memberId;
    private Long reportId;
    private ReportCategory reportCategory;
    private String reportImageUrl;
    private String title;
    private Double latitude;
    private Double longitude;
    private String address;
    private int viewCount;
}
