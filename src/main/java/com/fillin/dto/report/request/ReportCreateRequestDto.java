package com.fillin.dto.report.request;

import com.fillin.domain.enums.ReportCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCreateRequestDto {
    private String title;
    private String content;
    private Double latitude;
    private Double longitude;
    private ReportCategory category; // DANGER, INCONVENIENCE, DISCOVERY 등
}