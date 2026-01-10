package com.fillin.controller.mypage;

import com.fillin.dto.mypage.response.ReportCountResponseDto;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.mypage.MyReportService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/reports")
public class MyReportController {
    private final MyReportService myReportService;

    @Operation(summary = "제보 및 조회수 확인", description = "유저의 총 제보 갯수와 조회수를 확인합니다.")
    @GetMapping("/count")
    public Response<ReportCountResponseDto> getReportCount(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.countReport(memberId);
    }
}
