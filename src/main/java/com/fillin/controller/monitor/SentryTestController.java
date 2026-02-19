package com.fillin.controller.monitor;

import io.sentry.Sentry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SentryTestController {

//    @GetMapping("/test-error")
//    public String throwError() {
//
//        // 일부러 런타임 에러를 발생시킵니다.
//        throw new RuntimeException("Sentry 연동 성공! - FILLIN 프로젝트 에러 테스트");
//    }
}
