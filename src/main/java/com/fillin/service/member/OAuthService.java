package com.fillin.service.member;

import com.fillin.dto.member.request.SocialAuthRequest;
import com.fillin.dto.member.response.SocialAuthResponse;
import com.fillin.dto.member.response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface OAuthService {

    SocialAuthResponse socialLogin(SocialAuthRequest.LoginReq req);
    TokenResponse completeOnboarding(Long memberId, SocialAuthRequest.OnboardingReq req);
    void logout(String refreshToken);
    TokenResponse reissue(String refreshToken);
    SocialAuthResponse googleLoginWeb(String code);
    SocialAuthResponse googleLoginAndroid(String code);
}