package com.fillin.repository.member;

import com.fillin.domain.MemberAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {
    boolean existsByMemberIdAndAgreementId(Long memberId, Long agreementId);
}
