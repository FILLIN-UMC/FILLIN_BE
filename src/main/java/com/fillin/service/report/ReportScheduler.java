package com.fillin.service.report;

import com.fillin.domain.AlarmContext;
import com.fillin.domain.Member;
import com.fillin.domain.Report;
import com.fillin.domain.enums.AlarmType;
import com.fillin.domain.enums.FeedbackType;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.domain.enums.ValidType;
import com.fillin.global.event.AlarmEvent;
import com.fillin.repository.feedback.FeedbackRepository;
import com.fillin.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final ReportRepository reportRepository;
    private final FeedbackRepository feedbackRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시 실행
    public void notifyExpiringReports() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetStart = now.plusDays(3).with(LocalTime.MIN);
        LocalDateTime targetEnd = now.plusDays(3).with(LocalTime.MAX);

        List<Report> expiringReports = reportRepository.findByExpiresAtBetween(targetStart, targetEnd);
        
        Map<Member, List<Report>> reportsByMember = expiringReports.stream()
                .collect(Collectors.groupingBy(Report::getMember));

        reportsByMember.forEach((member, reports) -> {
            Map<ReportCategory, Long> countsByCategory = reports.stream()
                    .collect(Collectors.groupingBy(Report::getCategory, Collectors.counting()));

            StringBuilder messageBody = new StringBuilder("내 제보가 3일 뒤 사라져요 ");
            String categoryString = countsByCategory.entrySet().stream()
                    .map(entry -> getCategoryLabel(entry.getKey()) + entry.getValue())
                    .collect(Collectors.joining(", "));

            messageBody.append(categoryString);

            AlarmContext ctx = AlarmContext.builder()
                    .count(3)
                    .build();

            eventPublisher.publishEvent(new AlarmEvent(
                    member.getId(),
                    AlarmType.EXPIRATION,
                    ctx,
                    null
            ));

        });
    }

    // 만료 시간 지난 제보 status 변경
    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void updateExpiredReportsStatus() {
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = reportRepository.updateStatusToExpired(now);

        log.info("스케줄러 실행: 만료 시간이 지난 제보 {}건의 상태를 EXPIRED로 변경했습니다. (기준 시간: {})",
                updatedCount, now);
    }

    // 2주 지난 제보 오래된 제보일 수 있음 반환
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void updateReportValidType(){
        // 현재 시간으로부터 2주 전 시간 계산
        LocalDateTime threshold = LocalDateTime.now().minusWeeks(2);

        // 2주가 지난 제보들의 validType을 INVALID로 변경
        int updatedCount = reportRepository.updateValidTypeToInvalid(threshold);

        log.info("스케줄러 실행: 생성된 지 2주가 지난 제보 {}건의 validType을 INVALID로 변경했습니다. (기준 시간: {})",
                updatedCount, threshold);
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void updateReportStatusAndValidType() {
        LocalDateTime now = LocalDateTime.now();

        // [기준 1] 사라지는 기준: 7일간 부정 의견 3건 이상 -> 만료 시간 3일 후로
        List<Report> suspiciousReports = reportRepository.findReportsWithHighNegativeFeedback(now.minusDays(7));
        suspiciousReports.forEach(report -> report.updateExpiresAt(now.plusDays(3)));

        // [기준 2] 안 유효: 2주 이상 된 제보 -> INVALID (벌크 쿼리)
        reportRepository.updateOldReportsToInvalid(now.minusWeeks(2));

        // [기준 3] 현재 유효 / 중간 상태 (3일 유지 판단)
        List<Report> activeReports = reportRepository.findActiveReportsForValidation(now.minusWeeks(2));

        for (Report report : activeReports) {
            int positiveCount = feedbackRepository.countByReportIdAndType(report.getId(), FeedbackType.NOW);
            int negativeCount = feedbackRepository.countByReportIdAndType(report.getId(), FeedbackType.DONE);
            int totalCount = positiveCount + negativeCount;

            if (totalCount > 0) {
                double ratio = (double) positiveCount / totalCount * 100;
                ValidType targetType = null;

                // 현재 비율에 따른 타겟 타입 결정
                if (ratio >= 70.0) targetType = ValidType.VALID;
                else if (ratio >= 40.0 && ratio <= 60.0) targetType = ValidType.UNCERTAIN;

                if (targetType != null) {
                    // 이미 해당 타입이거나, 이미 그 타입으로 변하는 중인지 확인
                    if (report.getValidType() != targetType) {
                        if (report.getValidTypeModifiedAt() == null) {
                            // 처음 조건을 만족한 경우: 시작 시간 기록
                            report.updateValidTypeModifiedAt(now);
                        } else if (report.getValidTypeModifiedAt().isBefore(now.minusDays(3))) {
                            // 시작 시간으로부터 3일이 지났다면: 실제 상태 변경
                            report.updateValidType(targetType);
                            report.updateValidTypeModifiedAt(null); // 초기화
                        }
                    }
                } else {
                    // 비율 구간을 벗어나면 유지 시간 초기화
                    report.updateValidTypeModifiedAt(null);
                }
            }
        }
    }

    private String getCategoryLabel(ReportCategory category) {
        return switch (category) {
            case DANGER -> "위험";
            case INCONVENIENCE -> "불편";
            case DISCOVERY -> "발견";
        };
    }
}
