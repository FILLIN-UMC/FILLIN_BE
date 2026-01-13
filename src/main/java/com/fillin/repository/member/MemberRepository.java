package com.fillin.repository.member;

import com.fillin.domain.Member;
import com.fillin.domain.enums.SocialType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    public boolean existsByNickname(String nickname);
    Optional<Member> findBySocialTypeAndSocialId(SocialType socialType, String socialId);
}
