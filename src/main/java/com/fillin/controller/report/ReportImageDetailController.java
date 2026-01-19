package com.fillin.controller.report;

import com.fillin.domain.Member;
import com.fillin.domain.enums.FeedbackType;
import com.fillin.dto.report.response.LikeResponseDto;
import com.fillin.dto.report.response.ReportImageDetailResponseDto;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.global.security.annotation.AuthUser;
import com.fillin.service.report.ReportImageDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
@Tag(name = "Report Detail API", description = "제보 단건 조회 관련 API - by 황신애")
public class ReportImageDetailController {

    private final ReportImageDetailService reportImageDetailService;

    @Operation(summary = "제보 단건 조회 (상세)", description = "지도 화면 클릭 시 제보를 상세 조회합니다.")
    @GetMapping("/{reportId}/detail")
    public Response<ReportImageDetailResponseDto> getReportDetail(@PathVariable Long reportId) {
        return reportImageDetailService.getReportImageDetail(reportId);
    }

    @Operation(summary = "피드백 생성", description = "제보에 대한 피드백을 생성합니다.")
    @PostMapping("/{reportId}/feedback")
    @SecurityRequirement(name = "Bearer Content")
    public Response<String> createFeedback(@PathVariable Long reportId, @AuthUser Member member, FeedbackType type){
        Long memberId = member.getId();
        return reportImageDetailService.createFeedback(memberId, reportId, type);
    }

    @Operation(summary = "좋아요 토글", description = "제보에 대한 좋아요(저장) 토글입니다. 본인의 제보에 좋아요를 누를 수 없습니다.")
    @PostMapping("/{reportId}/like")
    @SecurityRequirement(name = "Bearer Content")
    public Response<LikeResponseDto> likeToggle(@PathVariable Long reportId, @AuthUser Member member){
        Long memberId = member.getId();
        return reportImageDetailService.toggleLike(memberId, reportId);
    }
}
