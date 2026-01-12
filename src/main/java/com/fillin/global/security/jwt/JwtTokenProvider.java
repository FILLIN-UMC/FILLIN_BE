package com.fillin.global.security.jwt;


import com.fillin.domain.Member;
import com.fillin.domain.enums.SocialType;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.security.exception.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

/**
 *  Jwt 생성, 검증, 파싱 Provider
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    private JwtParser jwtParser;
    private Key secretKey;

    //온보딩 전 회원을 위한 임시 토큰
    @Value("${jwt.onboarding-token-validity:900000}") // 15분 기본
    private long onboardingTokenValidity;

    public String createOnboardingToken(Long memberId, SocialType socialType) {
        return generateToken(memberId, null, socialType, onboardingTokenValidity, "onboarding");
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build();
    }

    public String createAccessToken(Member member, String email,SocialType socialType) {
        try {
            return generateToken(member.getId(), email, socialType, accessTokenValidity,"access");
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    public String createRefreshToken(Member member, String email,SocialType socialType) {
        try {
            return generateToken(member.getId(), email, socialType, refreshTokenValidity, "refresh");
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    private String generateToken(Long memberId, String email, SocialType socialType,
                                 long validity, String category) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + validity);

            return Jwts.builder()
                    .setSubject(email)
                    .claim("memberId", memberId)
                    .claim("category", category)      // access / refresh
                    .claim("socialType", socialType.name())
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(secretKey, SignatureAlgorithm.HS512)
                    .compact();
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }



    public boolean validateToken(String token) {
        String raw = resolve(token);
        if (raw == null) return false;

        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            log.info("Invalid JWT token", e);
            return false;}
    }

    // JWT 의 payload(Claims 객체) 추출
    public Claims getClaimsFromToken(String token) {
        String raw = resolve(token);
        if (raw == null){
            throw new AuthException(ErrorCode.JWT_TOKEN_NOT_FOUND);
        }

        try {
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            throw new AuthException(ErrorCode.JWT_EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
    }


    public Long getMemberIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Long memberId = claims.get("memberId", Long.class);
        if (memberId == null) throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        return memberId;
    }

    public String getCategory(String token) {
        return getClaimsFromToken(token).get("category", String.class);
    }

    public SocialType getSocialType(String token) {
        String v = getClaimsFromToken(token).get("socialType", String.class);
        return v == null ? null : SocialType.valueOf(v);
    }

    private String resolve(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        token = token.trim();
        if (token.startsWith("Bearer ")) return token.substring(7).trim();
        return token;
    }


}
