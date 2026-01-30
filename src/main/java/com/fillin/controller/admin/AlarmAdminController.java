package com.fillin.controller.admin;

import com.fillin.dto.alarm.request.NoticeRequest;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.alarm.AlarmAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자", description = "시스템 알람 API")
@RestController
@RequestMapping("/api/admin/alarms")
@RequiredArgsConstructor
public class AlarmAdminController {

    private final AlarmAdminService alarmAdminService;

    @PostMapping("/notice")
    @Operation(summary = "시스템 공지 알림 전송")
    public Response<?> sendNotice(@RequestBody NoticeRequest request) {
        alarmAdminService.sendNotice(request);
        return Response.ok(ResultCode.OK);
    }
}
