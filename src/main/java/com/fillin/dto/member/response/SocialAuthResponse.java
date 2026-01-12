package com.fillin.dto.member.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialAuthResponse {

    private boolean needOnboarding;
    // 온보딩 필요 시
    private String tempToken;
    // 온보딩 완료 시
    private TokenResponse token;
}