package com.fillin.dto.report.response;

import com.fillin.domain.enums.ReportCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularReportResponse {
    private Long id;
    private ReportCategory category;
    private String title;
    private Double latitude;
    private Double longitude;
    private int viewCount;
    private String address;
}
