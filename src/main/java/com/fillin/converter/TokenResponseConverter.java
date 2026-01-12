package com.fillin.converter;


import com.fillin.dto.member.response.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class TokenResponseConverter {

    public TokenResponse toResponse(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}