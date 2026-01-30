package com.fillin.service.member;

import com.fillin.converter.TokenResponseConverter;
import com.fillin.domain.Member;
import com.fillin.domain.NotificationSetting;
import com.fillin.domain.enums.Role;
import com.fillin.domain.enums.SocialType;
import com.fillin.dto.member.request.AuthRequest;
import com.fillin.dto.member.response.AuthResponse;
import com.fillin.dto.member.response.TokenResponse;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.security.exception.AuthException;
import com.fillin.global.security.jwt.JwtTokenProvider;
import com.fillin.repository.NotiSetRepository;
import com.fillin.repository.member.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenResponseConverter tokenResponseConverter;
    private final NotiSetRepository notiSetRepository;

    @Transactional
    @Override
    public void signupTestUser(AuthRequest.SignUpReq req) {
        // 1) 중복 체크
        if (memberRepository.existsByEmail(req.getEmail())) {
            throw new AuthException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(req.getPassword());

        // 3) 유저 생성 (LOCAL/DEMO)
        Member member = Member.builder()
                .email(req.getEmail())
                .password(encodedPassword)
                .nickname(req.getNickname())
                .socialType(SocialType.LOCAL)
                .onboarded(true)
                .build();

        memberRepository.save(member);

        NotificationSetting noti = NotificationSetting.builder()
                .member(member)
                .isServiceAlarm(true)
                .isReportAlarm(true)
                .isFeedbackAlarm(true)
                .build();

        notiSetRepository.save(noti);

    }

    @Override
    @Transactional
    public TokenResponse loginTestUser(AuthRequest.LogInReq req, HttpServletResponse response) {
        // 1) 유저 조회
        Member member = memberRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.getPassword(), member.getPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member,member.getEmail(),SocialType.LOCAL);
        String refreshToken = jwtTokenProvider.createRefreshToken(member, member.getEmail(),SocialType.LOCAL);

        member.updateRefreshToken(refreshToken);
        memberRepository.save(member);          // 명시적 저장

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("X-Refresh-Token", refreshToken);


        return tokenResponseConverter.toResponse(accessToken, refreshToken);
    }

    //개발 테스트용도라 reissue 추가안함
}