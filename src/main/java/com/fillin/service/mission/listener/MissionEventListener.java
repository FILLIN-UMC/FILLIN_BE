package com.fillin.service.mission.listener;

import com.fillin.domain.AlarmContext;
import com.fillin.domain.Member;
import com.fillin.domain.enums.Achievement;
import com.fillin.domain.enums.AlarmType;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.domain.enums.rank.Boangwan;
import com.fillin.domain.enums.rank.Haegyeolsa;
import com.fillin.domain.enums.rank.Tamheomga;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.event.AlarmEvent;
import com.fillin.global.event.ReportCreatedEvent;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionEventListener {
    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void handleReportCreated(ReportCreatedEvent event) {
        Member member = memberRepository.findById(event.memberId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        updateMemberRank(member, event.category());
    }

    // 랭크 업데이트 로직
    private void updateMemberRank(Member member, ReportCategory category) {

        // 총 제보 수
        int totalCount = reportRepository.countByMemberId(member.getId());

        Achievement newAchieve = Achievement.resolveAchievement(totalCount);
        if (member.getAchievement() != newAchieve) {
            member.updateAchievement(newAchieve);

            // 🔔 LEVEL_UP 알림 전송
            AlarmContext ctx = AlarmContext.builder()
                    .badgeName(getAchievementLabel(newAchieve))
                    .count(totalCount)                          // 총 제보 수
                    .build();

            eventPublisher.publishEvent(new AlarmEvent(
                    member,                 // 본인에게
                    AlarmType.LEVEL_UP,      // 등급 상승
                    ctx,
                    null                    // 랜딩 없음 (마이페이지)
            ));
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

    private String getAchievementLabel(Achievement achievement) {
        return switch (achievement) {
            case ROOKIE -> "루키";
            case VETERAN -> "베테랑";
            case MASTER -> "마스터";
        };
    }

}
