package com.fillin.controller.mypage;

import com.fillin.domain.Member;
import com.fillin.dto.mypage.response.*;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.global.security.annotation.AuthUser;
import com.fillin.service.mypage.MyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    public Response<ReportCountResponseDto> getReportCount(@AuthUser Member member) {
        Long memberId = member.getId();
        return myReportService.countReport(memberId);
    }

    @Operation(summary = "카테고리별 제보 갯수 확인", description = "유저의 카테고리별 제보 갯수를 확인합니다.")
    @GetMapping("/category")
    public Response<MyReportCategoryResponseDto> getMyReportCategory(@AuthUser Member member) {
        Long memberId = member.getId();
        return myReportService.getMyReportCategory(memberId);
    }

    @Operation(summary = "만료 예정 제보(간단)", description = "5일내로 만료 예정인 제보들")
    @GetMapping("/soon")
    public Response<ReportExpireSoonDto> getReportExpireSoon(@AuthUser Member member) {
        Long memberId = member.getId();
        return myReportService.getExpireSoon(memberId);
    }

    @Operation(summary = "만료 예정 제보(상세)", description = "5일내로 만료 예정인 제보들을 상세하게 조회합니다.")
    @GetMapping("/soon/detail")
    public Response<List<ReportExpireSoonDetailDto>> getReportExpireSoonDetail(@AuthUser Member member) {
        Long memberId = member.getId();
        return myReportService.getReportExpireSoonDetail(memberId);
    }

    @Operation(summary = "나의 제보", description = "유저가 한 제보 리스트 중 만료되지 않은 제보를 조회합니다.")
    @GetMapping
    public Response<List<MyReportListResponseDto>> getMyReportList(@AuthUser Member member) {
        Long memberId = member.getId();
        return myReportService.getMyReportList(memberId);
    }

    @Operation(summary = "나의 제보 (만료) ", description = "유저의 제보 중 만료된 제보를 조회합니다.")
    @GetMapping("/expired")
    public Response<List<MyReportListResponseDto>> getMyReportExpired(@AuthUser Member member) {
        Long memberId = member.getId();
        return myReportService.getMyReportListExpired(memberId);
    }

    @Operation(summary = "제보 삭제", description = "유저의 제보를 삭제(만료로 변경)합니다.")
    @PostMapping("{reportId}/expired")
    public Response<String> deleteReport(@AuthUser Member member,@PathVariable Long reportId) {
        Long memberId = member.getId();
        return myReportService.deleteMyReport(memberId,reportId);
    }

    @Operation(summary = "저장한 제보", description = "유저가 저장한 제보를 조회합니다.")
    @GetMapping("/like")
    public Response<List<MyReportListResponseDto>> getMyReportLike(@AuthUser Member member) {
        Long memberId = member.getId();
        return myReportService.getMyLikeReports(memberId);
    }
}
