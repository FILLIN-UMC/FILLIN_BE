package com.fillin.dto.member.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class GoogleResponse {

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

        @JsonProperty("aud")
        private String aud;
    }
}