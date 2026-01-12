package com.fillin.domain;

import com.fillin.domain.common.BaseTimeEntity;
import com.fillin.domain.enums.MemberStatus;
import com.fillin.domain.enums.Rank;
import com.fillin.domain.enums.Role;
import com.fillin.domain.enums.SocialType;
import com.fillin.dto.mypage.request.ProfileRequestDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    //온보딩 전이라 이메일, 닉네임 null 허용)
    @Column(unique = true)
    private String email;

    @Column(length = 15)
    private String nickname;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    private String socialId; // 소셜 로그인 식별값

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "member_ranks", // 생성될 테이블 이름
            joinColumns = @JoinColumn(name = "member_id") // 외래키 설정
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "rank_name")
    private List<Rank> ranks; // 등급

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    //소셜로그인 후 정상 회원 확인
    @Column(nullable = false)
    private boolean onboarded;

    @Column(length = 300)
    private String refreshToken;

    public void updateProfileInfo(ProfileRequestDto dto) {
        // 닉네임이 요청에 포함된 경우에만 업데이트
        if (dto.getNickname() != null) {
            this.nickname = dto.getNickname();
        }
        // 프로필 이미지가 요청에 포함된 경우에만 업데이트
        if (dto.getProfileImageUrl() != null) {
            this.profileImageUrl = dto.getProfileImageUrl();
        }
    }

    public static Member createSocialMember(SocialType socialType, String socialId, String email) {
        Member member = new Member();
        member.socialType = socialType;
        member.socialId = socialId;
        member.email = email;          // null 가능(온보딩 전)
        member.onboarded = false;      // 기본값
        return member;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken; // logout -> null 가능
    }

    //온보딩 작업 완료시
    public void markOnboarded() {
        this.onboarded = true;
    }

    //온보딩에서 닉네임 설정
    public void updateNicknameAndEmail(String nickname,String email) {
        this.nickname = nickname;
        this.email = email;
    }

}

