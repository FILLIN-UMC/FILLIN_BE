package com.fillin.global.security.exception;


import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.response.Response;
import lombok.Getter;

@Getter
public class AuthFailureHandler extends RuntimeException {
    private final ErrorCode errorCode;

    public AuthFailureHandler(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public Response<String> toResponse() {
        return new Response<>(false, errorCode.getCode(), errorCode.getMessage());
    }
}
