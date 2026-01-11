package com.fillin.service.mypage;

import com.fillin.domain.Member;
import com.fillin.domain.Report;
import com.fillin.domain.enums.FeedbackType;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.dto.mypage.response.*;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.repository.feedback.FeedbackRepository;
import com.fillin.repository.like.LikeRepository;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MyReportService {

    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final LikeRepository likeRepository;
    private final FeedbackRepository feedbackRepository;

    //총 제보 갯수 & 조회수
    @Transactional(readOnly = true)
    public Response<ReportCountResponseDto> countReport(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        ReportCountResponseDto dto = ReportCountResponseDto.builder()
                .memberId(member.getId())
                .totalReportCount(reportRepository.countByMemberId(memberId))
                .totalViewCount(reportRepository.totalViewCountByMemberId(memberId))
                .build();

        return Response.ok(ResultCode.OK,dto);
    }

    //나의 제보 (카테고리별)
    @Transactional(readOnly = true)
    public Response<MyReportCategoryResponseDto> getMyReportCategory(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        int danger = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.DANGER);
        int inconven = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.INCONVENIENCE);
        int discovery = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.DISCOVERY);

        MyReportCategoryResponseDto dto = MyReportCategoryResponseDto.builder()
                .memberId(member.getId())
                .dangerCount(danger)
                .inconvenienceCount(inconven)
                .discoveryCount(discovery)
                .build();

        return Response.ok(ResultCode.OK,dto);
    }

    //사라질 제보 (팝업)
    @Transactional(readOnly = true)
    public Response<ReportExpireSoonDto> getExpireSoon(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveDaysLater = now.plusDays(5);

        int danger = reportRepository.countByMemberIdAndCategoryAndExpiresAtBetween(memberId, ReportCategory.DANGER, now, fiveDaysLater);
        int inconven = reportRepository.countByMemberIdAndCategoryAndExpiresAtBetween(memberId, ReportCategory.INCONVENIENCE, now, fiveDaysLater);
        int discovery = reportRepository.countByMemberIdAndCategoryAndExpiresAtBetween(memberId, ReportCategory.DISCOVERY, now, fiveDaysLater);

        List<Report> reports = reportRepository.findByMemberIdAndExpiresAtBetween(memberId, now, fiveDaysLater);

        List<ReportExpireSoonDto.ReportExpireSoonListDto> dtos = reports.stream()
                .map(report -> {

                    return ReportExpireSoonDto.ReportExpireSoonListDto.builder()
                            .reportId(report.getId())
                            .reportCategory(report.getCategory())
                            .reportImageUrl(report.getReportImageUrl())
                            .expireTime(report.getExpiresAt())
                            .build();
                })
                .collect(Collectors.toList());

        ReportExpireSoonDto dto = ReportExpireSoonDto.builder()
                .memberId(member.getId())
                .dangerCount(danger)
                .inconvenienceCount(inconven)
                .discoveryCount(discovery)
                .listDtos(dtos)
                .build();

        return Response.ok(ResultCode.OK, dto);
    }

    //사라질 제보 (상세)
    @Transactional(readOnly = true)
    public Response<List<ReportExpireSoonDetailDto>> getReportExpireSoonDetail(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveDaysLater = now.plusDays(5);

        List<Report> reports = reportRepository.findByMemberIdAndExpiresAtBetween(memberId, now, fiveDaysLater);


        List<ReportExpireSoonDetailDto> dtos = reports.stream()
                .map(report -> {

                    int doneCount = feedbackRepository.countByReportIdAndType(report.getId(), FeedbackType.DONE);
                    int nowCount = feedbackRepository.countByReportIdAndType(report.getId(), FeedbackType.NOW);

                    return ReportExpireSoonDetailDto.builder()
                            .memberId(member.getId())
                            .achievement(member.getAchievement())
                            .profileImageUrl(member.getProfileImageUrl())
                            .reportId(report.getId())
                            .reportCategory(report.getCategory())
                            .validType(report.getValidType())
                            .reportImageUrl(report.getReportImageUrl())
                            .expireTime(report.getExpiresAt())
                            .title(report.getTitle())
                            .address(report.getAddress())
                            .createAt(report.getCreatedAt())
                            .latitude(report.getLatitude())
                            .longitude(report.getLongitude())
                            .viewCount(report.getViewCount())
                            .nowCount(nowCount)
                            .doneCount(doneCount)
                            .build();
                })
                .toList();

        return Response.ok(ResultCode.OK, dtos);
    }

    //나의 제보 (유지)
    @Transactional(readOnly = true)
    public Response<List<MyReportListResponseDto>> getMyReportList(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        List<Report> reports = reportRepository.findByMemberIdAndExpiresAtAfter(memberId, LocalDateTime.now());

        List<MyReportListResponseDto> dtos = reports.stream()
                .map(
                        report -> {
                            return MyReportListResponseDto.builder()
                                    .memberId(member.getId())
                                    .reportId(report.getId())
                                    .reportCategory(report.getCategory())
                                    .reportImageUrl(report.getReportImageUrl())
                                    .address(report.getAddress())
                                    .title(report.getTitle())
                                    .latitude(report.getLatitude())
                                    .longitude(report.getLongitude())
                                    .viewCount(report.getViewCount())
                                    .build();
                        }
                ).toList();

        return Response.ok(ResultCode.OK,dtos);
    }

    //니의 제보 (만료)
    @Transactional(readOnly = true)
    public Response<List<MyReportListResponseDto>> getMyReportListExpired(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        List<Report> reports = reportRepository.findByMemberIdAndExpiresAtBefore(memberId, LocalDateTime.now());

        List<MyReportListResponseDto> dtos = reports.stream()
                .map(
                        report -> {
                            return MyReportListResponseDto.builder()
                                    .memberId(member.getId())
                                    .reportId(report.getId())
                                    .reportCategory(report.getCategory())
                                    .reportImageUrl(report.getReportImageUrl())
                                    .address(report.getAddress())
                                    .title(report.getTitle())
                                    .latitude(report.getLatitude())
                                    .longitude(report.getLongitude())
                                    .viewCount(report.getViewCount())
                                    .build();
                        }
                ).toList();

        return Response.ok(ResultCode.OK,dtos);
    }

    //나의 제보 삭제
    public Response<String> deleteMyReport(Long memberId, Long reportId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

        reportRepository.delete(report);

        return Response.ok(ResultCode.OK,"report id = " + reportId + " deleted.");
    }


    //저장한 제보
    @Transactional(readOnly = true)
    public Response<List<MyReportListResponseDto>> getMyLikeReports(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        List<Report> reports = reportRepository.findAllByMemberIdInLikes(memberId);

        List<MyReportListResponseDto> dtos = reports.stream()
                .map(
                        report -> {
                            return MyReportListResponseDto.builder()
                                    .memberId(member.getId())
                                    .reportId(report.getId())
                                    .reportCategory(report.getCategory())
                                    .reportImageUrl(report.getReportImageUrl())
                                    .address(report.getAddress())
                                    .title(report.getTitle())
                                    .latitude(report.getLatitude())
                                    .longitude(report.getLongitude())
                                    .viewCount(report.getViewCount())
                                    .build();
                        }
                ).toList();

        return Response.ok(ResultCode.OK,dtos);
    }

}
