package com.fillin.controller.report;

import com.fillin.domain.Report;
import com.fillin.dto.report.request.ReportCreateRequestDto;
import com.fillin.dto.report.request.SearchResultReportRequest;
import com.fillin.dto.report.response.PopularReportListResponse;
import com.fillin.dto.report.response.PopularReportResponse;
import com.fillin.dto.report.response.ReportAnalysisResponseDto;
import com.fillin.dto.report.response.SearchResultReportResponse;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.report.ReportAnalysisService;
import com.fillin.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Report API", description = "제보 관련 API - by 문정우")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportAnalysisService reportAnalysisService;

    @Operation(summary = "제보 하기", description = "사용자가 새로운 제보를 등록합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<Long> createReport(
            @AuthenticationPrincipal UserDetails userDetails, // Security 설정 가정
            @RequestPart("request") ReportCreateRequestDto request, // JSON 데이터
            @RequestPart(value = "image", required = false) MultipartFile image // 파일 데이터
    ) {
        Long memberId = Long.valueOf(userDetails.getUsername());
        Long reportId = reportService.createReport(memberId, request, image);

        return Response.ok(ResultCode.OK, reportId);
    }

    @Operation(summary = "인기 제보 조회", description = "검색 화면에서 좋아요와 최신 순으로 총 6개의 제보를 조회합니다.")
    @GetMapping("/popular")
    public Response<PopularReportListResponse> popularReports() {

        List<PopularReportResponse> reports = reportService.getPopularReports();

        PopularReportListResponse response = new PopularReportListResponse(reports);

        return Response.ok(ResultCode.OK, response);
    }

    @Operation(summary = "제보 검색 결과 조회", description = "검색 키워드와 맞는 결과를 조회합니다.")
    @GetMapping("/search/results")
    public Response<List<SearchResultReportResponse>> searchResults(
            @ParameterObject SearchResultReportRequest request
    ) {

        List<SearchResultReportResponse> reports = reportService.getSearchResultReports(request);

        return Response.ok(ResultCode.OK, reports);
    }

    @Operation(summary = "제보 이미지 AI 분석", description = "이미지를 업로드하면 AI가 제보 제목과 카테고리(DANGER, INCONVENIENCE, DISCOVERY)를 추천해줍니다.")
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<ReportAnalysisResponseDto> analyzeReportImage(@RequestPart("image") MultipartFile image) {
        ReportAnalysisResponseDto result = reportAnalysisService.analyzeImage(image);
        return Response.ok(result); // ResultCode 없이 바로 객체를 넣는 경우 Response.ok(result) 사용 가능 (Response 구현에 따라 다름)
        // 만약 Response.ok(code, data) 형태만 지원한다면 아래처럼 쓰세요:
        // return Response.ok(ResultCode.OK, result);
    }
}