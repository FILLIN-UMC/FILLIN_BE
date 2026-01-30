package com.fillin.controller.report;

import com.fillin.dto.report.request.HotReportRequest;
import com.fillin.dto.report.response.HotReportResponse;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.report.HotReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "HotReport Place API", description = "내주변 인기장소 API - by 박종찬")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class HotReportController {

    private final HotReportService hotreportService;

    @Operation(summary = "내 주변 인기장소 발견", description = "사용자의 현재 위치 기반 3km 이내 인기장소 조회(6개)")
    @GetMapping("/hot")
    public Response<List<HotReportResponse>> getHotReports(
            @ModelAttribute HotReportRequest request
    ) {
        List<HotReportResponse> hotReports = hotreportService.getHotReports(request);
        return Response.ok(ResultCode.OK, hotReports);
    }
}