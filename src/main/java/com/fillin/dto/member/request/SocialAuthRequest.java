package com.fillin.dto.member.request;

import com.fillin.domain.enums.SocialType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

public class SocialAuthRequest {
    @Getter
    @Setter
    @NoArgsConstructor
    public static class LoginReq {
        @NotNull
        private SocialType socialType; // KAKAO, GOOGLE

        @NotNull
        private String accessToken;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GoogleLoginReq {
        @NotNull
        private String code;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class OnboardingReq {

        @NotNull
        private String nickname;

        @NotNull
        private String email;

        @NotNull
        private List<Long> agreedAgreementIds;
    }
}
