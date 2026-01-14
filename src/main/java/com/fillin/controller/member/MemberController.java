package com.fillin.controller.member;

import com.fillin.dto.alarm.request.FcmTokenRequest;
import com.fillin.service.member.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/fcm-token")
    @Operation(summary = "fcm 토큰 등록")
    public void saveFcmToken(
            @Valid @RequestBody FcmTokenRequest request
    ) {
        memberService.updateFcmToken(request.getFcmToken());
    }
}
