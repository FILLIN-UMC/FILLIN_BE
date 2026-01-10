package com.fillin.dto.mypage.response;

import com.fillin.domain.enums.ReportCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExpireSoonDto {
    private Long memberId;
    private List<ReportExpireSoonListDto> listDtos;
    private int dangerCount;
    private int inconvenienceCount;
    private int discoveryCount;

    @Getter
    @Builder
    private static class ReportExpireSoonListDto {
        private Long reportId;
        private ReportCategory reportCategory;
        private String reportImageUrl;
        private LocalDateTime expireTime;
    }
}
