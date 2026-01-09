package com.fillin.domain.searchHistory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SearchHistoryCreateRequest {
    @NotBlank
    private String keyword;
}
