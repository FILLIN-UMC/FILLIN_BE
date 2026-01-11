package com.fillin.service.mission;

import com.fillin.domain.Member;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.domain.enums.rank.Boangwan;
import com.fillin.domain.enums.rank.Haegyeolsa;
import com.fillin.domain.enums.rank.Tamheomga;
import com.fillin.dto.mypage.response.MissionStatusDto;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MissionService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;

    public Response<List<MissionStatusDto>> getMemberMissionStatus(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 1. 각 카테고리별 실시간 카운트 조회
        int dangerCount = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.DANGER);
        int inconvenCount = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.INCONVENIENCE);
        int discoveryCount = reportRepository.countByMemberIdAndCategory(memberId, ReportCategory.DISCOVERY);

        // 2. DTO 생성
        List<MissionStatusDto> dtos = List.of(
                new MissionStatusDto(ReportCategory.DANGER, dangerCount, Boangwan.getNextTarget(dangerCount)),
                new MissionStatusDto(ReportCategory.INCONVENIENCE, inconvenCount, Haegyeolsa.getNextTarget(inconvenCount)),
                new MissionStatusDto(ReportCategory.DISCOVERY, discoveryCount, Tamheomga.getNextTarget(discoveryCount))
        );

        return Response.ok(ResultCode.OK,dtos);
    }
}
