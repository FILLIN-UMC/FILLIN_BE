package com.fillin.service.member;

import com.fillin.converter.TokenResponseConverter;
import com.fillin.domain.Agreement;
import com.fillin.domain.Member;
import com.fillin.domain.MemberAgreement;
import com.fillin.domain.NotificationSetting;
import com.fillin.domain.enums.Achievement;
import com.fillin.domain.enums.SocialType;
import com.fillin.domain.enums.rank.Boangwan;
import com.fillin.domain.enums.rank.Haegyeolsa;
import com.fillin.domain.enums.rank.Tamheomga;
import com.fillin.dto.member.request.SocialAuthRequest;
import com.fillin.dto.member.response.GoogleResponse;
import com.fillin.dto.member.response.KakaoResponse;
import com.fillin.dto.member.response.SocialAuthResponse;
import com.fillin.dto.member.response.TokenResponse;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.security.exception.AuthException;
import com.fillin.global.security.jwt.JwtTokenProvider;
import com.fillin.global.util.oauth.GoogleUtil;
import com.fillin.global.util.oauth.KakaoUtil;
import com.fillin.repository.NotiSetRepository;
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
@Transactional(readOnly = true)
public class OAuthServiceImpl implements OAuthService {

    private final MemberRepository memberRepository;
    private final MemberAgreementRepository memberAgreementRepository; // 온보딩에서 약관 저장할 때 사용
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenResponseConverter tokenResponseConverter;
    private final AgreementRepository agreementRepository;
    private final NotiSetRepository notiSetRepository;

    /**
     * Facade에서 이미 검증된 이메일/ID를 받아 DB 작업을 수행
     */
    @Override
    @Transactional
    public SocialAuthResponse loginOrSignup(SocialType socialType, String socialId, String email) {

        Member member = memberRepository
                .findBySocialTypeAndSocialId(socialType, socialId)
                .orElseGet(() -> memberRepository.save(
                        Member.createSocialMember(socialType, socialId, email)
                ));

        if (!isOnboarded(member)) {
            String tempToken = jwtTokenProvider.createOnboardingToken(member.getId(), socialType);
            return SocialAuthResponse.builder()
                    .needOnboarding(true)
                    .tempToken(tempToken)
                    .build();
        }

        String accessToken = jwtTokenProvider.createAccessToken(member, member.getEmail(), socialType);
        String refreshToken = jwtTokenProvider.createRefreshToken(member, member.getEmail(), socialType);

        member.updateRefreshToken(refreshToken); // Dirty Checking

        return SocialAuthResponse.builder()
                .needOnboarding(false)
                .token(tokenResponseConverter.toResponse(accessToken, refreshToken))
                .build();
    }

    @Override
    @Transactional
    public TokenResponse completeOnboarding(Long memberId, SocialAuthRequest.OnboardingReq req) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(req.getNickname())) {
            throw new AuthException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        validateRequiredAgreements(req.getAgreedAgreementIds());
        // 이메일, 닉네임 저장
        member.updateNicknameAndEmail(req.getNickname(),req.getEmail());
        // 이용약관 동의 여부 저장
        saveAgreements(member, req.getAgreedAgreementIds());
        // 온보딩 완료 체크
        member.markOnboarded();

        member.updateAchievement(Achievement.ROOKIE);
        member.updateBoangwan(Boangwan.BOANGWAN_0);
        member.updateHaegyeolsa(Haegyeolsa.HAEGYEOLSA_0);
        member.updateTamheomga(Tamheomga.TAMHEOMGA_0);

        // 알림 설정 엔티티 설정 (처음엔 다 on)
        NotificationSetting noti = NotificationSetting.builder()
                .member(member)
                .isServiceAlarm(true)
                .isReportAlarm(true)
                .isFeedbackAlarm(true)
                .build();

        notiSetRepository.save(noti);

        SocialType socialType = member.getSocialType();
        String accessToken = jwtTokenProvider.createAccessToken(member, member.getEmail(), socialType);
        String refreshToken = jwtTokenProvider.createRefreshToken(member, member.getEmail(), socialType);

        member.updateRefreshToken(refreshToken);

        return tokenResponseConverter.toResponse(accessToken, refreshToken);
    }

    private void validateRequiredAgreements(List<Long> agreedIds) {

        List<Agreement> requiredAgreements = agreementRepository.findByRequiredTrue();

        for (Agreement required : requiredAgreements) {
            if (!agreedIds.contains(required.getId())) {
                // 필수 약관 중 하나라도 동의 목록에 없으면 에러 발생
                throw new AuthException(ErrorCode.AGREEMENT_REQUIRED);
            }
        }
    }

    @Transactional
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

        SocialType socialType = member.getSocialType();
        String newAccess = jwtTokenProvider.createAccessToken(member, member.getEmail(), socialType);
        String newRefresh = jwtTokenProvider.createRefreshToken(member, member.getEmail(), socialType);
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