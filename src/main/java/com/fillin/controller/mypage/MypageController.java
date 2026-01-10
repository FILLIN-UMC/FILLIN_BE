package com.fillin.controller.mypage;

import com.fillin.dto.mypage.request.ProfileRequestDto;
import com.fillin.dto.mypage.response.ProfileResponseDto;
import com.fillin.dto.mypage.response.ReportCountResponseDto;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.mypage.MypageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
@Tag(name = "Mainpage API", description = "메인페이지 관련 API - by 황신애")
public class MypageController {

    private final MypageService mypageService;

    //TODO : 추후 유저 관련 시큐리티& JWT 설정 완료시 변경

    @Operation(summary = "유저 프로필 조회", description = "유저의 프로필을 조회합니다")
    @GetMapping("/profile")
    public Response<ProfileResponseDto> getProfile(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return mypageService.getProfile(memberId);
    }

    @Operation(summary = "유저 프로필 변경", description = "유저의 프로필을 변경합니다.")
    @PostMapping("/profile/edit")
    public Response<ProfileResponseDto> updateProfile(@Param("memberId") Long memberId, ProfileRequestDto profileRequestDto) {
        // Long memberId = member.getMember().getId();
        return mypageService.updateProfile(memberId, profileRequestDto);
    }

    @Operation(summary = "유저 닉네임 중복 조회", description = "변경하려는 닉네임의 중복을 확인합니다.")
    @GetMapping("/profile/check")
    public Response<String> checkDupNickname(@Param("memberId") Long memberId, @Param("nickname") String nickname) {
        // Long memberId = member.getMember().getId();
        return mypageService.checkDuplicateNickname(memberId, nickname);
    }

    @Operation(summary = "제보 및 조회수 확인", description = "유저의 총 제보 갯수와 조회수를 확인합니다.")
    @GetMapping("/reports/count")
    public Response<ReportCountResponseDto> getReportCount(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return mypageService.countReport(memberId);
    }


}
