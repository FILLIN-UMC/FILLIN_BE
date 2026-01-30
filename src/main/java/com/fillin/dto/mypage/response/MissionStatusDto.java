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
public class MissionStatusDto {
    private ReportCategory category;
    private int currentCount;
    private int targetCount;
}
