package com.fillin.service.mission.listener;

import com.fillin.domain.Member;
import com.fillin.domain.MemberMission;
import com.fillin.domain.enums.Achievement;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.domain.enums.rank.Boangwan;
import com.fillin.domain.enums.rank.Haegyeolsa;
import com.fillin.domain.enums.rank.Tamheomga;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.global.event.ReportCreatedEvent;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.mission.MemberMissionRepository;
import com.fillin.repository.mission.MissionRepository;
import com.fillin.repository.report.ReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionEventListener {
    private final MemberRepository memberRepository;
    private final MissionRepository missionRepository;
    private final MemberMissionRepository memberMissionRepository;
    private final ReportRepository reportRepository;

    @EventListener
    @Transactional
    public void handleReportCreated(ReportCreatedEvent event) {
        Member member = memberRepository.findById(event.memberId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 1. 미션 진행도 업데이트
        updateMissionProgress(member, event.category());

        // 2. 랭크 재산정 및 업데이트
        updateMemberRank(member, event.category());
    }

    // 미션 진행도 증가 로직
    private void updateMissionProgress(Member member, ReportCategory category) {
        // 진행 중인 미션 중 해당 카테고리와 일치하는 미션 조회
        List<MemberMission> missions = memberMissionRepository.findByMemberAndMission_CategoryAndIsCompleteFalse(member, category);

        for (MemberMission mm : missions) {
            mm.addProgress(); // 진행도 +1 (MemberMission 엔티티에 메서드 추가 필요)

            // 목표 달성 확인
            if (mm.getProgressCount() >= mm.getMission().getTargetCount()) {
                mm.complete(); // 완료 처리 (MemberMission 엔티티에 메서드 추가 필요)
            }
        }
    }

    // 랭크 업데이트 로직
    private void updateMemberRank(Member member, ReportCategory category) {

        //총 제보 수
        int totalCount = reportRepository.countByMemberId(member.getId());

        Achievement newAchieve = Achievement.resolveAchievement(totalCount);
        if(member.getAchievement() != newAchieve){
            member.updateAchievement(newAchieve);
        }

        // 해당 카테고리의 총 제보 수 조회 (ReportRepository 활용)
        int categoryCount = reportRepository.countByMemberIdAndCategory(member.getId(), category);

        switch (category) {
            case DANGER -> {
                Boangwan newRank = Boangwan.resolveBoangwanRank(categoryCount);
                if (member.getBoangwan() != newRank) {
                    member.updateBoangwan(newRank);
                }
            }
            case INCONVENIENCE -> {
                Haegyeolsa newRank = Haegyeolsa.resolveHaegyeolsaRank(categoryCount);
                if (member.getHaegyoelsa() != newRank) {
                    member.updateHaegyeolsa(newRank);
                }
            }
            case DISCOVERY -> {
                Tamheomga newRank = Tamheomga.resolveTamheomgaRank(categoryCount);
                if (member.getTamheomga() != newRank) {
                    member.updateTamheomga(newRank);
                }
            }
        }
    }


}
