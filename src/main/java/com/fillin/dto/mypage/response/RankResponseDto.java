package com.fillin.dto.mypage.response;

import com.fillin.domain.enums.Rank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankResponseDto {
    private List<Rank> ranks;
}
