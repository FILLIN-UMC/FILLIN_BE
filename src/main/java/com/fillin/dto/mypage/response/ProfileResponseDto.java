package com.fillin.dto.mypage.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDto {
    private Long memberId;
    private String nickname;
    private String profileImageUrl;
    private List<String> ranks;
}
