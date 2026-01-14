package com.fillin.service.member;

import com.fillin.converter.TokenResponseConverter;
import com.fillin.domain.Agreement;
import com.fillin.domain.Member;
import com.fillin.domain.MemberAgreement;
import com.fillin.domain.enums.SocialType;
import com.fillin.dto.member.request.SocialAuthRequest;
import com.fillin.dto.member.response.KakaoResponse;
import com.fillin.dto.member.response.SocialAuthResponse;
import com.fillin.dto.member.response.TokenResponse;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.security.exception.AuthException;
import com.fillin.global.security.jwt.JwtTokenProvider;
import com.fillin.global.util.oauth.KakaoUtil;
import com.fillin.repository.agreement.AgreementRepository;
import com.fillin.repository.member.MemberAgreementRepository;
import com.fillin.repository.member.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OAuthServiceImpl implements OAuthService {

    private final MemberRepository memberRepository;
    private final MemberAgreementRepository memberAgreementRepository; // 온보딩에서 약관 저장할 때 사용
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenResponseConverter tokenResponseConverter;
    private final AgreementRepository agreementRepository;
    private final KakaoUtil kakaoUtil; // ✅ 팩토리 대신 유틸 직접 주입

    @Override
    public SocialAuthResponse socialLogin(SocialAuthRequest.LoginReq req, HttpServletResponse response) {

        // 지금은 카카오만 먼저 할 거라서 가드
        if (req.getSocialType() != SocialType.KAKAO) {
            throw new AuthException(ErrorCode.UNSUPPORTED_SOCIAL_TYPE);
        }

        // 1) code -> kakao access token
        KakaoResponse.TokenResponse kakaoToken = kakaoUtil.requestAccessTokenWithCode(req.getCode());

        // 2) access token -> kakao profile
        KakaoResponse.KakaoProfile profile = kakaoUtil.requestProfile(kakaoToken.getAccessToken());

        // 3) 소셜 식별자(socialId) 추출
        String socialId = String.valueOf(profile.getId());

        String email;
        if (profile.getKakaoAccount() != null) {
            email = profile.getKakaoAccount().getEmail(); // null 가능
        } else {
            email = null;
        }

        // 4) 회원 조회 or 생성 (socialType + socialId 기준)
        Member member = memberRepository
                .findBySocialTypeAndSocialId(SocialType.KAKAO, socialId)
                .orElseGet(() -> memberRepository.save(
                        Member.createSocialMember(SocialType.KAKAO, socialId, email)
                ));

        // 5) 온보딩 여부 판단
        if (!isOnboarded(member)) {
            String tempToken = jwtTokenProvider.createOnboardingToken(member.getId(),SocialType.KAKAO);
            return SocialAuthResponse.builder()
                    .needOnboarding(true)
                    .tempToken(tempToken)
                    .build();
        }

        // 6) 온보딩 완료: 최종 JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(member,email,SocialType.KAKAO);
        String refreshToken = jwtTokenProvider.createRefreshToken(member,email,SocialType.KAKAO);

        member.updateRefreshToken(refreshToken);
        memberRepository.save(member);

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("X-Refresh-Token", refreshToken);

        return SocialAuthResponse.builder()
                .needOnboarding(false)
                .token(tokenResponseConverter.toResponse(accessToken, refreshToken))
                .build();
    }

    @Override
    public TokenResponse completeOnboarding(Long memberId, SocialAuthRequest.OnboardingReq req) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(req.getNickname())) {
            throw new AuthException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        member.updateNicknameAndEmail(req.getNickname(),req.getEmail());

        // 약관 동의 저장 로직은 너희 엔티티/리포지토리에 맞춰 구현
        saveAgreements(member, req.getAgreedAgreementIds());

        member.markOnboarded();

        String accessToken = jwtTokenProvider.createAccessToken(member,member.getEmail(),SocialType.KAKAO);
        String refreshToken = jwtTokenProvider.createRefreshToken(member,member.getEmail(),SocialType.KAKAO);

        member.updateRefreshToken(refreshToken);

        return tokenResponseConverter.toResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String raw = stripBearer(refreshToken);
        Long memberId = jwtTokenProvider.getMemberIdFromToken(raw);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (member.getRefreshToken() == null || !member.getRefreshToken().equals(raw)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        member.updateRefreshToken(null);
    }

    @Override
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String raw = stripBearer(refreshToken);

        if (!jwtTokenProvider.validateToken(raw)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        Long memberId = jwtTokenProvider.getMemberIdFromToken(raw);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (member.getRefreshToken() == null || !member.getRefreshToken().equals(raw)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String newAccess = jwtTokenProvider.createAccessToken(member,member.getEmail(),SocialType.KAKAO);
        String newRefresh = jwtTokenProvider.createRefreshToken(member,member.getEmail(),SocialType.KAKAO);

        member.updateRefreshToken(newRefresh);

        return tokenResponseConverter.toResponse(newAccess, newRefresh);
    }

    private boolean isOnboarded(Member member) {
        return member.isOnboarded() && member.getNickname() != null;
    }

    private void saveAgreements(Member member, List<Long> agreedAgreementIds) {

        List<Agreement> agreements = agreementRepository.findAllById(agreedAgreementIds);

        if (agreements.size() != agreedAgreementIds.size()) {
            throw new AuthException(ErrorCode.AGREEMENT_NOT_FOUND);
        }

        for (Agreement agreement : agreements) {
            if (memberAgreementRepository.existsByMemberIdAndAgreementId(member.getId(), agreement.getId())) {
                continue;
            }
            memberAgreementRepository.save(MemberAgreement.agree(member, agreement));
        }
    }

    private String stripBearer(String token) {
        token = token.trim();
        return token.startsWith("Bearer ") ? token.substring(7).trim() : token;
    }
}