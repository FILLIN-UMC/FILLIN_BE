package com.fillin.dto.mypage.response;

import com.fillin.domain.enums.Achievement;
import com.fillin.domain.enums.rank.Boangwan;
import com.fillin.domain.enums.rank.Haegyeolsa;
import com.fillin.domain.enums.rank.Tamheomga;
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
    private Achievement achievement;
    private Boangwan boangwan;
    private Haegyeolsa haegyeolsa;
    private Tamheomga tamheomga;
}
