package com.fillin.service.alarm;

import com.fillin.domain.Alarm;
import com.fillin.domain.Member;
import com.fillin.domain.enums.AlarmType;
import com.fillin.global.event.AlarmEvent;
import com.fillin.repository.alarm.AlarmRepository;
import com.fillin.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmEventListener {

    private final AlarmRepository alarmRepository;
    private final MemberRepository memberRepository;

    @Async("alarmExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AlarmEvent event) {

        try {

            AlarmType type = event.getAlarmType();

            Member receiver = memberRepository.getReferenceById(event.getReceiverId());


            Alarm alarm = Alarm.builder()
                    .member(receiver)
                    .alarmType(type)
                    .message(type.buildMessage(event.getContext()))
                    .referId(event.getReferId())
                    .isRead(false)
                    .build();

            alarmRepository.save(alarm);
        } catch (Exception e) {
            log.error("알림 비동기 처리 실패 event={}",event, e);
        }
    }
}

