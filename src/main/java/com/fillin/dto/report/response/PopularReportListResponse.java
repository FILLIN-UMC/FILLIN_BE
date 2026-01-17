package com.fillin.dto.report.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PopularReportListResponse {
    private List<PopularReportResponse> popularReports;
}
