package com.fillin.service.member;

import com.fillin.dto.member.request.AuthRequest;
import com.fillin.dto.member.response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    void signupTestUser(AuthRequest.SignUpReq req);
    TokenResponse loginTestUser(AuthRequest.LogInReq request, HttpServletResponse response);

}
