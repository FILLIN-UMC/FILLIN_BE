package com.fillin.service.report;

import com.fillin.converter.report.ReportConverter; // import 추가
import com.fillin.domain.Report;
import com.fillin.repository.report.ReportRepository;
import com.fillin.dto.report.request.HotReportRequest;
import com.fillin.dto.report.response.HotReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotReportService {

    private final ReportRepository reportRepository;

    public List<HotReportResponse> getHotReports(HotReportRequest request) {
        Pageable pageable = PageRequest.of(0, 6);

        List<Report> reports = reportRepository.findHotReports(
                request.getLatitude(),
                request.getLongitude(),
                pageable
        );

        // Converter를 사용하여 변환
        return reports.stream()
                .map(ReportConverter::toHotReportResponse)
                .collect(Collectors.toList());
    }
}