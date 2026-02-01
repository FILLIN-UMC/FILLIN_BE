package com.fillin.domain;

import com.fillin.domain.common.BaseTimeEntity;
import com.fillin.domain.enums.*;
import com.fillin.domain.enums.rank.Boangwan;
import com.fillin.domain.enums.rank.Haegyeolsa;
import com.fillin.domain.enums.rank.Tamheomga;
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

    // 온보딩 전이라 이메일, 닉네임 null 허용)
    @Column(unique = true)
    private String email;

    // 테스트 위한 개발용 로그인
    @Column
    private String password;

    @Column(length = 15)
    private String nickname;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    private String socialId; // 소셜 로그인 식별값

    @Enumerated(EnumType.STRING)
    private Achievement achievement;

    @Enumerated(EnumType.STRING)
    private Boangwan boangwan;

    @Enumerated(EnumType.STRING)
    private Haegyeolsa haegyoelsa;

    @Enumerated(EnumType.STRING)
    private Tamheomga tamheomga;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Column(length = 255)
    private String fcmToken;

    public Member(Long id, String nickname, String email, String profileImageUrl, SocialType socialType,
            String socialId,
            Achievement achievement, Role role, MemberStatus status, Boangwan boangwan, Haegyeolsa haegyeolsa,
            Tamheomga tamheomga) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.socialType = socialType;
        this.socialId = socialId;
        this.achievement = (achievement != null) ? achievement : Achievement.ROOKIE;
        this.boangwan = (boangwan != null) ? boangwan : Boangwan.BOANGWAN_0;
        this.haegyoelsa = (haegyeolsa != null) ? haegyeolsa : Haegyeolsa.HAEGYEOLSA_0;
        this.tamheomga = (tamheomga != null) ? tamheomga : Tamheomga.TAMHEOMGA_0;
        this.role = role;
        this.status = status;
    }

    public void updateAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

    public void updateBoangwan(Boangwan newRank) {
        this.boangwan = newRank;
    }

    public void updateHaegyeolsa(Haegyeolsa newRank) {
        this.haegyoelsa = newRank;
    }

    public void updateTamheomga(Tamheomga newRank) {
        this.tamheomga = newRank;
    }

    // 소셜로그인 후 정상 회원 확인
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
        member.email = email; // null 가능(온보딩 전)
        member.onboarded = false; // 기본값
        return member;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken; // logout -> null 가능
    }

    // 온보딩 작업 완료시
    public void markOnboarded() {
        this.onboarded = true;
    }

    // 온보딩에서 닉네임 설정
    public void updateNicknameAndEmail(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
    }

    // 알림 FCM 토큰
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    //탈퇴 메서드
    public void withdraw() {
        this.status = MemberStatus.INACTIVE; // 상태를 비활성으로 변경
        this.refreshToken = null;            // 리프레시 토큰 삭제
        this.fcmToken = null;                // 푸시 알림 토큰 삭제
        this.nickname = "탈퇴한 사용자";
        this.profileImageUrl = null;
        this.email = null;
        this.socialId = null;
    }

}
