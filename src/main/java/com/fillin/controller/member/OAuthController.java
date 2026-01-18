package com.fillin.controller.member;

import com.fillin.domain.enums.SocialType;
import com.fillin.dto.member.request.SocialAuthRequest;
import com.fillin.dto.member.response.SocialAuthResponse;
import com.fillin.dto.member.response.TokenResponse;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.global.security.annotation.AuthUser;
import com.fillin.service.member.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "소셜 로그인/온보딩")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;


    @Operation(summary = "카카오 로그인", description = "인가 코드(code)로 카카오 로그인 후 토큰 또는 온보딩 토큰을 반환합니다.")
    @PostMapping("/kakao/login")
    public Response<SocialAuthResponse> kakaoLogin(
            @Valid @RequestBody SocialAuthRequest.LoginReq request,
            jakarta.servlet.http.HttpServletResponse response
    ) {

        request.setSocialType(com.fillin.domain.enums.SocialType.KAKAO);

        SocialAuthResponse res = oAuthService.socialLogin(request, response);
        return Response.ok(ResultCode.OK, res);
    }
    @Operation(summary = "구글 로그인 (WEB 리다이렉트)", description = "인가 코드(code)로 구글 로그인(WEB 리다이렉트 플로우) 후 토큰 또는 온보딩 토큰을 반환합니다.")
    @PostMapping("/google/web/login")
    public Response<SocialAuthResponse> googleLoginWeb(
            @Valid @RequestBody SocialAuthRequest.GoogleLoginReq request,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        SocialAuthResponse res = oAuthService.googleLoginWeb(request.getCode(), response);
        return Response.ok(ResultCode.OK, res);
    }

    @Operation(summary = "구글 로그인 (ANDROID)", description = "인가 코드(code)로 구글 로그인(Android serverAuthCode 플로우) 후 토큰 또는 온보딩 토큰을 반환합니다.")
    @PostMapping("/google/android/login")
    public Response<SocialAuthResponse> googleLoginAndroid(
            @Valid @RequestBody SocialAuthRequest.GoogleLoginReq request,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        SocialAuthResponse res = oAuthService.googleLoginAndroid(request.getCode(), response);
        return Response.ok(ResultCode.OK, res);
    }

    @Operation(summary = "온보딩 완료", description = "온보딩 토큰으로 인증 후 이메일/닉네임/약관동의를 저장하고 정식 토큰을 발급합니다.")
    @PostMapping("/onboarding")
    public Response<TokenResponse> completeOnboarding(
            @AuthUser Long memberId,
            @Valid @RequestBody SocialAuthRequest.OnboardingReq request
    ) {
        TokenResponse res = oAuthService.completeOnboarding(memberId, request);
        return Response.ok(ResultCode.OK, res);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 검증한 후 새로운 Access/Refresh Token을 발급합니다.")
    @PostMapping("/reissue")
    public Response<TokenResponse> reissue(@RequestHeader("X-Refresh-Token") String refreshToken) {
        TokenResponse res = oAuthService.reissue(refreshToken);
        return Response.ok(ResultCode.OK, res);
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화합니다.")
    @PatchMapping("/logout")
    public Response<String> logout(@RequestHeader("X-Refresh-Token") String refreshToken) {
        oAuthService.logout(refreshToken);
        return Response.ok(ResultCode.OK, "로그아웃이 완료되었습니다.");
    }
    // 테스트용 콜백 컨트롤러
    @GetMapping("/kakao/callback")
    public ResponseEntity<SocialAuthResponse> callback(
            @RequestParam("code") String code,
            HttpServletResponse servletResponse
    ) {
        SocialAuthRequest.LoginReq req = new SocialAuthRequest.LoginReq();
        req.setSocialType(SocialType.KAKAO);
        req.setCode(code);

        SocialAuthResponse result = oAuthService.socialLogin(req, servletResponse);
        return ResponseEntity.ok(result);
    }


}