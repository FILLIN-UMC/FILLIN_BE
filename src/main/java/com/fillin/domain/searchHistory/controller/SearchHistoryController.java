package com.fillin.domain.searchHistory.controller;

import com.fillin.domain.Member;
import com.fillin.domain.searchHistory.dto.request.SearchHistoryCreateRequest;
import com.fillin.domain.searchHistory.dto.response.RecentSearchListResponse;
import com.fillin.domain.searchHistory.dto.response.SearchHistoryResponse;
import com.fillin.domain.searchHistory.entity.SearchHistory;
import com.fillin.domain.searchHistory.service.SearchHistoryService;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.exception.BusinessException;
import com.fillin.global.apiPayload.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping("/recent/list")
    public Response<RecentSearchListResponse> getRecentSearches(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false) LocalDateTime cursorTime,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size) {
        if (member == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (size <= 0 || size > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if ((cursorTime == null) != (cursorId == null)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Slice<SearchHistory> slice = searchHistoryService.getRecentSearches(member, cursorTime, cursorId, size);
        List<SearchHistoryResponse> items = slice.getContent()
                .stream()
                .map(SearchHistoryResponse::from)
                .toList();

        return Response.ok(new RecentSearchListResponse(items, slice.hasNext()));
    }

    @PostMapping("/recent")
    public Response<Void> saveSearchKeyword(
            @AuthenticationPrincipal Member member,
            @RequestBody SearchHistoryCreateRequest request
    ) {
        if (member == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        searchHistoryService.saveSearchKeyword(member, request.getKeyword().trim());
        
        return Response.ok();
    }
}
