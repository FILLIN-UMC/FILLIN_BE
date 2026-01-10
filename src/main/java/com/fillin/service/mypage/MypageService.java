package com.fillin.service.mypage;

import com.fillin.domain.Member;
import com.fillin.domain.Report;
import com.fillin.dto.mypage.request.ProfileRequestDto;
import com.fillin.dto.mypage.response.ProfileResponseDto;
import com.fillin.dto.mypage.response.RankResponseDto;
import com.fillin.dto.mypage.response.ReportCountResponseDto;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.code.ResultCode;
import com.fillin.global.apiPayload.exception.GlobalException;
import com.fillin.global.apiPayload.response.Response;
import com.fillin.repository.bookmark.BookmarkRepository;
import com.fillin.repository.member.MemberRepository;
import com.fillin.repository.report.ReportRepository;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final BookmarkRepository bookmarkRepository;

    //프로필 조회
    public Response<ProfileResponseDto> getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        ProfileResponseDto dto = ProfileResponseDto.builder()
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .build();

        return Response.ok(ResultCode.OK,dto);
    }

    //랭크 조회
    public Response<RankResponseDto> getRank(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        RankResponseDto dto = RankResponseDto.builder()
                .achievement(member.getAchievement())
                .boangwan(member.getBoangwan())
                .tamheomga(member.getTamheomga())
                .haegyeolsa(member.getHaegyoelsa())
                .build();

        return Response.ok(ResultCode.OK,dto);
    }

    //프로필 수정
    public Response<ProfileResponseDto> updateProfile(Long memberId, ProfileRequestDto profileRequestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        member.updateProfileInfo(profileRequestDto);

        ProfileResponseDto dto = ProfileResponseDto.builder()
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .build();

        return Response.ok(ResultCode.OK,dto);
    }

    //닉네임 중복 확인
    public Response<String> checkDuplicateNickname(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if(memberRepository.existsByNickname(nickname)) {
            return Response.fail(ErrorCode.DUPLICATE_NICKNAME);
        }
        else return Response.ok(ResultCode.OK,nickname + "은 사용가능한 닉네임입니다.");
    }

    //총 제보 갯수 & 조회수
    public Response<ReportCountResponseDto> countReport(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new GlobalException(ErrorCode.USER_NOT_FOUND));

        ReportCountResponseDto dto = ReportCountResponseDto.builder()
                .totalReportCount(reportRepository.countByMemberId(memberId))
                .totalViewCount(reportRepository.totalViewCountByMemberId(memberId))
                .build();

        return Response.ok(ResultCode.OK,dto);
    }

}
