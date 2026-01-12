package com.fillin.controller.report;

import com.fillin.dto.report.request.ReportCreateRequestDto;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

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
}