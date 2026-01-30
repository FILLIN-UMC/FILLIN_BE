package com.fillin.controller.member;

import com.fillin.dto.member.request.AuthRequest;
import com.fillin.dto.member.response.TokenResponse;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.member.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth(Test)", description = "개발/테스트용 로컬 회원가입/로그인 - by 박종찬")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth/test")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "테스트 유저 회원가입", description = "개발용 테스트 유저 회원가입입니다")
    @PostMapping("/signup")
    public Response<String> signup(@Valid @RequestBody AuthRequest.SignUpReq req) {
        authService.signupTestUser(req);
        return Response.ok(ResultCode.OK,"회원가입이 완료되었습니다."); // 너희 공통 응답에 맞춰 수정 (예: Response.ok(SuccessCode.SIGNUP_OK))
    }

    @Operation(summary = "테스트 유저 로그인", description = "개발용 테스트 유저 로그인입니다. 이메일과 비밀번호를 입력해주세요.")
    @PostMapping("/login")
    public Response<TokenResponse> login(@Valid @RequestBody AuthRequest.LogInReq req,
                                            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.loginTestUser(req, response);
        return Response.ok(ResultCode.OK, tokenResponse);
    }
}