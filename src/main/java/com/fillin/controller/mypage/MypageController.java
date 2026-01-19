package com.fillin.controller.mypage;

import com.fillin.dto.mypage.request.ProfileRequestDto;
import com.fillin.dto.mypage.response.ProfileResponseDto;
import com.fillin.dto.mypage.response.RankResponseDto;
import com.fillin.dto.mypage.response.ReportCountResponseDto;
import com.fillin.dto.notiSet.request.NotificationUpdateRequestDto;
import com.fillin.dto.notiSet.response.NotificationUpdateResponseDto;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.service.mypage.MypageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
@Tag(name = "Mypage API", description = "마이페이지 관련 API - by 황신애")
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
    public Response<ProfileResponseDto> updateProfile(@Param("memberId") Long memberId, ProfileRequestDto profileRequestDto, @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        // Long memberId = member.getMember().getId();
        return mypageService.updateProfile(memberId, profileRequestDto, imageFile);
    }

    @Operation(summary = "유저 닉네임 중복 조회", description = "변경하려는 닉네임의 중복을 확인합니다.")
    @GetMapping("/profile/check")
    public Response<String> checkDupNickname(@Param("memberId") Long memberId, @Param("nickname") String nickname) {
        // Long memberId = member.getMember().getId();
        return mypageService.checkDuplicateNickname(memberId, nickname);
    }

    @Operation(summary = "유저 랭크 조회", description = "유저의 랭크를 조회합니다.")
    @GetMapping("/profile/ranks")
    public Response<RankResponseDto> getRanks(@Param("memberId") Long memberId) {
        // Long memberId = member.getMember().getId();
        return mypageService.getRank(memberId);
    }

    @Operation(summary = "유저 알림 설정 변경", description = "알림의 수신 여부를 변경합니다")
    @PostMapping("/profile/notiSet")
    public Response<NotificationUpdateResponseDto> updateNotiSet(Long memberId, NotificationUpdateRequestDto dto){
        // Long memberId = member.getMember().getId();
        return mypageService.updateNotiSet(memberId,dto);
    }

    @Operation(summary = "유저 알림 설정 조회", description = "알림의 수신 여부를 조회합니다")
    @GetMapping("/profile/notiSet")
    public Response<NotificationUpdateResponseDto> getNotiSet(Long memberId){
        // Long memberId = member.getMember().getId();
        return mypageService.getNotiSet(memberId);
    }

}
