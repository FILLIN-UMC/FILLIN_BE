package com.fillin.service.report;

import com.fillin.domain.Feedback;
import com.fillin.domain.Like;
import com.fillin.domain.Member;
import com.fillin.domain.Report;
import com.fillin.domain.enums.FeedbackType;
import com.fillin.dto.mypage.response.ReportExpireSoonDetailDto;
import com.fillin.dto.report.request.FeedbackRequestDto;
import com.fillin.dto.report.response.LikeResponseDto;
import com.fillin.dto.report.response.ReportImageDetailResponseDto;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.global.event.AlarmEvent;
import com.fillin.repository.feedback.FeedbackRepository;
import com.fillin.repository.like.LikeRepository;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportImageDetailService {

    private final ReportRepository reportRepository;
    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final LikeRepository likeRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 상세 조회
    @Transactional(readOnly = true)
    public Response<ReportImageDetailResponseDto> getReportImageDetail(Long reportId) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

        int doneCount = feedbackRepository.countByReportIdAndType(report.getId(), FeedbackType.DONE);
        int nowCount = feedbackRepository.countByReportIdAndType(report.getId(), FeedbackType.NOW);

        ReportImageDetailResponseDto dto = ReportImageDetailResponseDto.builder()
                .writerId(report.getMember().getId())
                .achievement(report.getMember().getAchievement())
                .profileImageUrl(report.getMember().getProfileImageUrl())
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

        report.addViewCount();

        return Response.ok(ResultCode.OK, dto);
    }

    // 피드백 생성 제보당 한 개만 가능, 이미 있으면 변경
    public Response<String> createFeedback(Long memberId, Long reportId, FeedbackType type) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

        if (feedbackRepository.existsByMemberIdAndReportId(memberId, report.getId())) {
            Feedback feedback = feedbackRepository.findByMemberIdAndReportId(memberId, report.getId());
            feedback.updateFeedbackType(type);
        } else {
            Feedback feedback = Feedback.builder()
                    .member(member)
                    .report(report)
                    .type(type)
                    .build();

            feedbackRepository.save(feedback);
        }

        // 알림 전송 (Case 1)
        if (!report.getMember().getId().equals(memberId)) {
            String feedbackLabel = getFeedbackLabel(report.getCategory(), type);
            eventPublisher.publishEvent(new AlarmEvent(
                    report.getMember(),
                    com.fillin.domain.enums.AlarmType.LIKE, // 피드백 알림은 LIKE 타입 사용 (설정에서 피드백 알림으로 묶임)
                    member.getNickname() + "님이 회원님의 제보에 '" + feedbackLabel + "' 피드백을 남겼어요!",
                    report.getId()));
        }

        return Response.ok(ResultCode.OK, "사용자가 제보 id = " + report.getId() + "에 " + type + "을 남겼습니다.");
    }

    private String getFeedbackLabel(com.fillin.domain.enums.ReportCategory category, FeedbackType type) {
        if (type == FeedbackType.DONE) {
            return switch (category) {
                case DANGER -> "이제 괜찮아요";
                case INCONVENIENCE -> "해결됐어요";
                case DISCOVERY -> "이제 없어요";
            };
        } else {
            return switch (category) {
                case DANGER -> "아직 위험해요";
                case INCONVENIENCE -> "아직 불편해요";
                case DISCOVERY -> "아직 있어요";
            };
        }
    }

    // 좋아요 토글
    public Response<LikeResponseDto> toggleLike(Long memberId, Long reportId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getMember().getId().equals(member.getId())) {
            return Response.fail(ErrorCode.LIKE_FORBIDDEN);
        }

        Like like = likeRepository.findByMemberIdAndReportId(memberId, reportId);

        if (like == null) {
            Like newLike = Like.builder()
                    .member(member)
                    .report(report)
                    .build();

            likeRepository.save(newLike);
            report.addLikeCount();

            // 알림 전송 (Case 2)
            eventPublisher.publishEvent(new AlarmEvent(
                    report.getMember(),
                    com.fillin.domain.enums.AlarmType.LIKE,
                    member.getNickname() + "님이 회원님의 제보를 저장했어요.",
                    report.getId()));
        } else {
            likeRepository.delete(like);
            report.removeLikeCount();
        }

        LikeResponseDto dto = LikeResponseDto.builder()
                .memberId(member.getId())
                .reportId(report.getId())
                .likeCount(report.getLikeCount())
                .build();

        return Response.ok(ResultCode.OK, dto);
    }

}
