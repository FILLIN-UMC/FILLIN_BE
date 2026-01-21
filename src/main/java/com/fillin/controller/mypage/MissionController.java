package com.fillin.controller.mypage;

import com.fillin.domain.Member;
import com.fillin.dto.mypage.response.MissionStatusDto;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.global.security.annotation.AuthUser;
import com.fillin.service.mission.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@RequestMapping("/api/mypage/missions")
@Tag(name = "Mission API", description = "미션 관련 API - by 황신애")
public class MissionController {
    private final MissionService missionService;

    @Operation(summary = "미션 진행도 조회", description = "각 카테고리 별 유저의 미션 진행도를 확인합니다.")
    @GetMapping
    public Response<List<MissionStatusDto>> getMyMissionStatus(@AuthUser Member member) {
        Long memberId = member.getId();
        return missionService.getMemberMissionStatus(memberId);
    }
}
