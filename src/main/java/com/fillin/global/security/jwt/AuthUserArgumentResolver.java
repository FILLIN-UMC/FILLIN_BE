package com.fillin.global.security.jwt;


import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.security.annotation.AuthUser;
import com.fillin.global.security.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthUserArgumentResolver(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(AuthUser.class) != null
                && parameter.getParameterType().equals(Long.class);
    }


    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);   // or custom exception
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long memberId) {     // principal 타입을 확실히 Long 으로 보장
            return memberId;
        }

        // 혹시 String 으로 저장된 경우 방어 코드
        if (principal instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                throw new AuthException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 그 외 타입은 전부 인증 에러 처리
        throw new AuthException(ErrorCode.UNAUTHORIZED);
    }
}

