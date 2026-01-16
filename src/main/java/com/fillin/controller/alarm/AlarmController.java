package com.fillin.controller.alarm;

import com.fillin.dto.alarm.response.AlarmResponse;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.alarm.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "알람", description = "알람 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/list")
    @Operation(summary = "알람 목록 조회")
    public Response<List<AlarmResponse>> getAlarms(
            @RequestParam(required = false) Boolean read) {
        List<AlarmResponse> result = alarmService.getAlarms(read);
        return Response.ok(ResultCode.OK, result);
    }

    @PatchMapping("/{alarmId}/read")
    @Operation(summary = "알림 읽음 처리")
    public Response<String> readAlarm(
            @PathVariable long alarmId) {
        alarmService.readAlarm(alarmId);
        return Response.ok(ResultCode.OK, "알림 읽음 처리 완료");
    }
}
