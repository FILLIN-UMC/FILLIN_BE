package com.fillin.dto.searchHistory.response;

import com.fillin.domain.SearchHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SearchHistoryResponse {

    @Schema(example = "1")
    private Long id;
    private String keyword;
    private LocalDateTime searchAt;

    public static SearchHistoryResponse from(SearchHistory history) {
        return new SearchHistoryResponse(
                history.getId(),
                history.getKeyword(),
                history.getSearchAt()
        );
    }
}