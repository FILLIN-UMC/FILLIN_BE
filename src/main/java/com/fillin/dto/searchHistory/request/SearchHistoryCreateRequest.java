package com.fillin.dto.searchHistory.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SearchHistoryCreateRequest {
    @NotBlank
    private String keyword;
}
