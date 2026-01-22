package com.fillin.dto.report.response;

import com.fillin.domain.enums.ReportCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportAnalysisResponseDto {
    private String title;
    private ReportCategory category;

    public ReportAnalysisResponseDto(String title, ReportCategory category) {
        this.title = title;
        this.category = category;
    }
}