package com.fillin.service.mypage;

import com.fillin.domain.Member;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.dto.mypage.response.MyReportCategoryResponseDto;
import com.fillin.dto.mypage.response.ReportExpireSoonDto;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.repository.like.LikeRepository;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class MyReportService {

    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final LikeRepository bookmarkRepository;

    //나의 제보 (카테고리별)
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
    public Response<ReportExpireSoonDto> getExpireSoon(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        LocalDate now = LocalDate.now();

        int danger = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.DANGER);
        int inconven = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.INCONVENIENCE);
        int discovery = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.DISCOVERY);

    }

    //사라질 제보 (상세)

    //나의 제보 (유지)

    //니의 제보 (만료)

    //나의 제보 삭제

    //저장한 제보

}
