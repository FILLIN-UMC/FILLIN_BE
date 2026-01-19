package com.fillin.global.util.oauth;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fillin.dto.member.response.KakaoResponse;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.security.exception.AuthFailureHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.HashSet;
import java.util.Set;


@Slf4j
@Component
public class KakaoUtil {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public KakaoUtil(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private Set<String> allowedRedirectUris;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.rest-api-key}")
    private String restapikey;

    @PostConstruct
    public void initAllowedRedirectUris() {
        this.allowedRedirectUris = new HashSet<>();
        allowedRedirectUris.add("http://localhost:8080/api/auth/kakao/callback"); // 개발용
        allowedRedirectUris.add(redirectUri); // 운영 환경
    }

    /** 인가 코드(code)로 카카오 액세스 토큰 요청 */
    public KakaoResponse.TokenResponse requestAccessTokenWithCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("rest_api_key", restapikey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        if (clientSecret != null && !clientSecret.isBlank()) {
            params.add("client_secret", clientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kauth.kakao.com/oauth/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[KAKAO] token status: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new AuthFailureHandler(ErrorCode.KAKAO_AUTH_FAILED);
            }

            return objectMapper.readValue(response.getBody(), KakaoResponse.TokenResponse.class);

        } catch (JsonProcessingException e) {
            throw new AuthFailureHandler(ErrorCode.KAKAO_JSON_PARSE_ERROR);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("[KAKAO] status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AuthFailureHandler(ErrorCode.KAKAO_API_ERROR);
        }
    }

    /** access token으로 카카오 사용자 정보 요청 */
    public KakaoResponse.KakaoProfile requestProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[KAKAO] profile status: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new AuthFailureHandler(ErrorCode.KAKAO_AUTH_FAILED);
            }

            return objectMapper.readValue(response.getBody(), KakaoResponse.KakaoProfile.class);

        } catch (JsonProcessingException e) {
            log.error("[🚨ERROR🚨] 카카오 프로필 파싱 오류: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.KAKAO_JSON_PARSE_ERROR);
        } catch (Exception e) {
            log.error("[🚨ERROR🚨] 카카오 프로필 요청 중 오류 발생: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.KAKAO_API_ERROR);
        }
    }
}

