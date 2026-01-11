package com.fillin.dto.report.request;

import com.fillin.domain.enums.FeedbackType;
import com.fillin.domain.enums.ReportCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequestDto {
    private Long reportId;
    private FeedbackType type;
}
