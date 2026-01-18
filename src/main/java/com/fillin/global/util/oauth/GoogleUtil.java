package com.fillin.global.util.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fillin.dto.member.response.GoogleResponse; // 위에서 만든 DTO
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.security.exception.AuthFailureHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class GoogleUtil {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GoogleUtil(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.token-uri:https://oauth2.googleapis.com/token}")
    private String tokenUrl;

    @Value("${google.resource-uri:https://www.googleapis.com/oauth2/v3/userinfo}")
    private String userInfoUrl;

    @Value("${google.redirect-uri:}")
    private String redirectUri;

    /** (WEB 리다이렉트 플로우) 인가 코드(code)로 구글 액세스 토큰 요청 */
    public GoogleResponse.TokenResponse requestAccessTokenWithCodeWeb(String code) {
        if (redirectUri == null || redirectUri.isBlank()) {
            log.error("[GOOGLE] redirectUri is blank. Set `google.redirect-uri` for web redirect flow.");
            throw new AuthFailureHandler(ErrorCode.GOOGLE_AUTH_FAILED);
        }
        return requestAccessTokenInternal(code, redirectUri);
    }

    /** (ANDROID Google Sign-In serverAuthCode 플로우) 인가 코드(code)로 구글 액세스 토큰 요청 */
    public GoogleResponse.TokenResponse requestAccessTokenWithCodeAndroid(String code) {
        // Android serverAuthCode 플로우는 redirect_uri가 없거나 빈 값으로 처리되는 케이스가 많아
        // 파라미터를 아예 보내지 않도록 한다.
        log.info("[GOOGLE-TOKEN-REQ] tokenUrl={}, grantType={}, redirectUri={}, clientIdPrefix={}, clientIdLen={}, clientSecretLen={}, codePrefix={}",
                tokenUrl,
                "authorization_code",
                redirectUri,
                clientId == null ? "null" : clientId.substring(0, Math.min(12, clientId.length())),
                clientId == null ? -1 : clientId.length(),
                clientSecret == null ? -1 : clientSecret.length(),
                code == null ? "null" : code.substring(0, Math.min(10, code.length()))
        );
        return requestAccessTokenInternal(code, null);
    }

    /** 공통 토큰 교환 로직 */
    private GoogleResponse.TokenResponse requestAccessTokenInternal(String code, String redirectUriOrNull) {

        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);

        // WEB 리다이렉트 방식은 반드시 redirect_uri가 필요하고,
        // Android serverAuthCode 방식은 redirect_uri를 보내지 않는 것이 안전하다.
        if (redirectUriOrNull != null && !redirectUriOrNull.isBlank()) {
            params.add("redirect_uri", redirectUriOrNull);
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[GOOGLE] token status: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new AuthFailureHandler(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            return objectMapper.readValue(response.getBody(), GoogleResponse.TokenResponse.class);

        } catch (JsonProcessingException e) {
            log.error("[🚨ERROR🚨] 구글 토큰 파싱 오류: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.GOOGLE_JSON_PARSE_ERROR);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("[GOOGLE] API Error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AuthFailureHandler(ErrorCode.GOOGLE_API_ERROR);
        }
    }



    /** access token으로 구글 사용자 정보 요청 */
    public GoogleResponse.GoogleProfile requestProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // "Bearer {token}" 자동 설정

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[GOOGLE] profile status: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new AuthFailureHandler(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            return objectMapper.readValue(response.getBody(), GoogleResponse.GoogleProfile.class);

        } catch (JsonProcessingException e) {
            log.error("[🚨ERROR🚨] 구글 프로필 파싱 오류: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.GOOGLE_JSON_PARSE_ERROR);
        } catch (Exception e) {
            log.error("[🚨ERROR🚨] 구글 프로필 요청 중 오류 발생: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.GOOGLE_API_ERROR);
        }
    }

}