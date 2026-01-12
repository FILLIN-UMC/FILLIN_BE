package com.fillin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name="uk_member_agreement", columnNames={"member_id","agreement_id"})
        }
)
public class MemberAgreement { // 회원-약관 매핑

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id",nullable = false)
    private Agreement agreement;

    @Column(nullable = false)
    private boolean agreed;

    @Column(nullable = false)
    private LocalDateTime agreedAt;

    //동의기록 메서드
    public static MemberAgreement agree(Member member, Agreement agreement) {
        MemberAgreement ma = new MemberAgreement();
        ma.member = member;
        ma.agreement = agreement;
        ma.agreed = true;
        ma.agreedAt = LocalDateTime.now();
        return ma;
    }
}
