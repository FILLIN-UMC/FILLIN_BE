package com.fillin.repository.member;

import com.fillin.domain.MemberAgreement;
import org.springframework.data.repository.CrudRepository;

public interface MemberAgreementRepository extends CrudRepository<MemberAgreement, Long> {
    boolean existsByMemberIdAndAgreementId(Long memberId, Long agreementId);
}
