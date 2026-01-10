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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 15)
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

    public Member(Long id, String nickname, String email, String profileImageUrl, SocialType socialType, String socialId,
                  Achievement achievement, Role role, MemberStatus status,Boangwan boangwan, Haegyeolsa haegyeolsa, Tamheomga tamheomga) {
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

    @Builder


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
}

