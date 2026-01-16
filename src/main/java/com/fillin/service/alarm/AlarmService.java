package com.fillin.service.alarm;

import com.fillin.domain.Alarm;
import com.fillin.dto.alarm.response.AlarmResponse;
import com.fillin.repository.alarm.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;

    @Transactional(readOnly = true)
    public List<AlarmResponse> getAlarms(Boolean read) {
        Long memberId = getCurrentMemberId();
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        if (read == null) {
            return alarmRepository.findRecentAlarms(memberId, threeMonthsAgo)
                    .stream()
                    .map(AlarmResponse::from)
                    .toList();
        }

        return alarmRepository.findRecentAlarmsByRead(memberId, read, threeMonthsAgo)
                .stream()
                .map(AlarmResponse::from)
                .toList();
    }

    // TODO: 추후 JWT에서 꺼내도록 교체
    private Long getCurrentMemberId() {
        return 1L;
    }

    @Transactional
    public void readAlarm(Long alarmId) {
        Long memberId = getCurrentMemberId();

        Alarm alarm = alarmRepository.findByIdAndMemberId(alarmId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("알람이 존재하지 않습니다."));

        if (Boolean.FALSE.equals(alarm.getIsRead())) {
            alarm.markAsRead();
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldAlarms() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        alarmRepository.deleteByCreatedAtBefore(threeMonthsAgo);
    }
}
