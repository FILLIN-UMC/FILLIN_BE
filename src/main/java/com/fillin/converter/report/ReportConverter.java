package com.fillin.converter.report;

import com.fillin.domain.Report;
import com.fillin.dto.report.response.HotReportResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // 유틸리티 클래스로 인스턴스 생성 방지
public class ReportConverter {

    public static HotReportResponse toHotReportResponse(Report report) {
        return HotReportResponse.builder()
                .reportId(report.getId())
                .title(report.getTitle())
                .imageUrl(report.getReportImageUrl())
                .likeCount(report.getLikeCount())
                .address(report.getAddress())
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .build();
    }
}