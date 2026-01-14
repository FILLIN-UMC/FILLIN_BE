package com.fillin.dto.member.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class AuthResponse {

    @Getter
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoinResultDTO {     // 가입 후 반환할 정보
        private String email;
        private String nickName;
        private String accessToken;
        private String refreshToken;
    }

}


