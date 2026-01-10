package com.fillin.controller.mypage;

import com.fillin.dto.mypage.response.*;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.mypage.MyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/reports")
@Tag(name = "MypageReport API", description = "내 제보 관련 API - by 황신애")
public class MyReportController {
    private final MyReportService myReportService;

    @Operation(summary = "제보 및 조회수 확인", description = "유저의 총 제보 갯수와 조회수를 확인합니다.")
    @GetMapping("/count")
    public Response<ReportCountResponseDto> getReportCount(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.countReport(memberId);
    }

    @Operation(summary = "카테고리별 제보 갯수 확인", description = "유저의 카테고리별 제보 갯수를 확인합니다.")
    @GetMapping("/category")
    public Response<MyReportCategoryResponseDto> getMyReportCategory(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.getMyReportCategory(memberId);
    }

    @GetMapping("/soon")
    public Response<ReportExpireSoonDto> getReportExpireSoon(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.getExpireSoon(memberId);
    }

    @GetMapping("/soon/detail")
    public Response<List<ReportExpireSoonDetailDto>> getReportExpireSoonDetail(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.getReportExpireSoonDetail(memberId);
    }

    @GetMapping
    public Response<List<MyReportListResponseDto>> getMyReportList(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.getMyReportList(memberId);
    }

    @GetMapping("/expired")
    public Response<List<MyReportListResponseDto>> getMyReportExpired(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.getMyReportListExpired(memberId);
    }

    @DeleteMapping("{reportId}")
    public Response<String> deleteReport(Long memberId ,@PathVariable Long reportId) {
        // Long memberId = member.getMember().getId();
        return myReportService.deleteMyReport(memberId,reportId);
    }

    @GetMapping("/like")
    public Response<List<MyReportListResponseDto>> getMyReportLike(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return myReportService.getMyLikeReports(memberId);
    }
}
