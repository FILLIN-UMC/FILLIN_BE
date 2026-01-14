package com.fillin.service.member;

import com.fillin.domain.Member;
import com.fillin.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public void updateFcmToken(String fcmToken) {
        Long memberId = getCurrentMemberId();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.updateFcmToken(fcmToken);
    }
    private Long getCurrentMemberId() {
        return 1L;
    }
}
