package com.fillin.service.report;

import com.fillin.domain.Feedback;
import com.fillin.domain.Member;
import com.fillin.domain.Report;
import com.fillin.domain.enums.FeedbackType;
import com.fillin.dto.mypage.response.ReportExpireSoonDetailDto;
import com.fillin.dto.report.request.FeedbackRequestDto;
import com.fillin.dto.report.response.ReportImageDetailResponseDto;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.repository.feedback.FeedbackRepository;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportImageDetailService {

    private final ReportRepository reportRepository;
    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;

    //상세 조회
    @Transactional(readOnly = true)
    public Response<ReportImageDetailResponseDto> getReportImageDetail(Long reportId){

        Report report = reportRepository.findById(reportId).orElseThrow(()-> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

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

        return Response.ok(ResultCode.OK,dto);
    }

    //피드백 생성 제보당 한 개만 가능, 이미 있으면 변경
    public Response<String> createFeedback(Long memberId, Long reportId, FeedbackType type){
        Member member = memberRepository.findById(memberId).orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));
        Report report = reportRepository.findById(reportId).orElseThrow(()-> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

        if(feedbackRepository.existsByMemberIdAndReportId(memberId, report.getId())){
            Feedback feedback = feedbackRepository.findByMemberIdAndReportId(memberId, report.getId());
            feedback.updateFeedbackType(type);
        }
        else{
            Feedback feedback = Feedback.builder()
                    .member(member)
                    .report(report)
                    .type(type)
                    .build();

            feedbackRepository.save(feedback);
        }

        return Response.ok(ResultCode.OK,"사용자가 제보 id = "+ report.getId() + "에 " + type + "을 남겼습니다." );
    }

}
