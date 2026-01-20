package com.fillin.dto.report.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class HotReportRequest {
    private Double latitude;  // 내 현재 위도
    private Double longitude; // 내 현재 경도
}
