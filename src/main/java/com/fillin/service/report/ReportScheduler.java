package com.fillin.service.report;

import com.fillin.domain.Member;
import com.fillin.domain.Report;
import com.fillin.domain.enums.AlarmType;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.global.event.AlarmEvent;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시 실행
    public void notifyExpiringReports() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetStart = now.plusDays(3).with(LocalTime.MIN);
        LocalDateTime targetEnd = now.plusDays(3).with(LocalTime.MAX);

        List<Report> expiringReports = reportRepository.findByExpiresAtBetween(targetStart, targetEnd);

        // 회원별로 그룹화하여 알림 전송
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

            eventPublisher.publishEvent(new AlarmEvent(
                    member,
                    AlarmType.EXPIRATION,
                    messageBody.toString(),
                    null // 특정 제보 ID가 아닌 목록 페이지로 랜딩되므로 referId는 null 혹은 적절한 값
            ));
        });
    }

    private String getCategoryLabel(ReportCategory category) {
        return switch (category) {
            case DANGER -> "위험";
            case INCONVENIENCE -> "불편";
            case DISCOVERY -> "발견";
        };
    }
}
