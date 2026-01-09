package com.fillin.dto.searchHistory.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecentSearchListResponse {
    private List<SearchHistoryResponse> data;
    private boolean hasNext;
}
