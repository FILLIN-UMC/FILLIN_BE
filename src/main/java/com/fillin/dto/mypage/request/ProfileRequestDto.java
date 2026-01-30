package com.fillin.dto.mypage.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequestDto {
    private String profileImageUrl;
    private String nickname;
}
