package com.fillin.service.report;

import com.fillin.domain.Member;
import com.fillin.domain.Report;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.domain.enums.ReportStatus;
import com.fillin.domain.enums.ValidType;
import com.fillin.dto.report.request.ReportCreateRequestDto;
import com.fillin.dto.report.request.SearchResultReportRequest;
import com.fillin.dto.report.response.PopularReportResponse;
import com.fillin.dto.report.response.SearchResultReportResponse;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.exception.BusinessException;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import com.fillin.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service; // ★ S3Service 주입

    public Long createReport(Long memberId, ReportCreateRequestDto requestDto, MultipartFile imageFile) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        // 1. 이미지 업로드 수행 (이미지가 있을 경우)
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3Service.uploadImage(imageFile); // ★ S3에 업로드하고 URL 받기
        }

        // 2. Report 엔티티 생성 및 저장
        Report report = Report.builder()
                .member(member)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .category(requestDto.getCategory())
                .reportImageUrl(imageUrl)
                .status(ReportStatus.PUBLISHED)
                .validType(ValidType.VALID)
                .expiresAt(LocalDateTime.now().plusDays(30)) // 제보 만료 기간: 30일
                .build();

        Report savedReport = reportRepository.save(report);
        return savedReport.getId();
    }

    public List<PopularReportResponse> getPopularReports() {

        List<Report> reports = reportRepository.findTop6ByCategoryInOrderByLikeCountDescCreatedAtDesc(
                List.of(ReportCategory.DISCOVERY, ReportCategory.INCONVENIENCE)
        );

        return reports.stream()
                .map(report -> new PopularReportResponse(
                        report.getId(),
                        report.getCategory(),
                        report.getTitle(),
                        report.getLatitude(),
                        report.getLongitude(),
                        report.getViewCount(),
                        report.getAddress()
                ))
                .toList();
    }

    public List<SearchResultReportResponse> getSearchResultReports(SearchResultReportRequest request) {

        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        if (request.getMinLatitude() == null || request.getMaxLatitude() == null
                || request.getMinLongitude() == null || request.getMaxLongitude() == null) {
            throw new BusinessException(ErrorCode.INVALID_LOCATION_RANGE);
        }

        if (request.getMinLatitude() > request.getMaxLatitude()
                || request.getMinLongitude() > request.getMaxLongitude()) {
            throw new BusinessException(ErrorCode.INVALID_LOCATION_RANGE);
        }

        List<Report> reports = reportRepository.findByKeywordAndLatitudeBetweenAndLongitudeBetween(
                request.getKeyword(),
                request.getMinLatitude(),
                request.getMaxLatitude(),
                request.getMinLongitude(),
                request.getMaxLongitude()
        );

        return reports.stream()
                .map(report -> new SearchResultReportResponse(
                        report.getId(),
                        report.getLatitude(),
                        report.getLongitude()
                ))
                .toList();
    }
}