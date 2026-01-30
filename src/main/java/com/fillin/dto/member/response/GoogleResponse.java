package com.fillin.dto.member.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class GoogleResponse {

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드가 와도 에러 안 나게 무시
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private int expiresIn;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("id_token")
        private String idToken;
    }


    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleProfile {
        @JsonProperty("sub")
        private String sub;      // 구글 고유 ID

        @JsonProperty("email")
        private String email;

        @JsonProperty("name")
        private String name;

        @JsonProperty("picture")
        private String picture;
    }
}