package com.fillin.dto.member.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AuthRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SignUpReq{
        @NotNull
        private String nickname;

        @NotNull
        private String email;

        @NotNull
        private String password;

        @NotNull
        private String confirmPassword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LogInReq{
        @NotNull
        private String email;

        @NotNull
        private String password;
    }

}
