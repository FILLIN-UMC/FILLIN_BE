package com.fillin.dto.mypage.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
//DANGER, INCONVENIENCE, DISCOVERY
public class MyReportCategoryResponseDto {
    private Long memberId;
    private int dangerCount;
    private int inconvenienceCount;
    private int discoveryCount;
}
