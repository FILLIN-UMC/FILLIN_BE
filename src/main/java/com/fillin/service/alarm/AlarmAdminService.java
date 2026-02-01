package com.fillin.service.alarm;

import com.fillin.domain.AlarmContext;
import com.fillin.domain.enums.AlarmType;
import com.fillin.dto.alarm.request.NoticeRequest;
import com.fillin.global.event.AlarmEvent;
import com.fillin.repository.alarm.AlarmRepository;
import com.fillin.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmAdminService {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void sendNotice(NoticeRequest request) {

        AlarmContext ctx = AlarmContext.builder()
                .message(request.getMessage())
                .build();

        memberRepository.findAll().forEach(member ->
                eventPublisher.publishEvent(
                        new AlarmEvent(
                                member.getId(),
                                AlarmType.NOTICE,
                                ctx,
                                null
                        )
                ));
    }
}
