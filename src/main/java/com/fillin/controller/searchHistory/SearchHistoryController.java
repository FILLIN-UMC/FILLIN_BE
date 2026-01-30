package com.fillin.controller.searchHistory;

import com.fillin.domain.Member;
import com.fillin.domain.SearchHistory;
import com.fillin.dto.searchHistory.request.SearchHistoryCreateRequest;
import com.fillin.dto.searchHistory.response.RecentSearchListResponse;
import com.fillin.dto.searchHistory.response.SearchHistoryResponse;
import com.fillin.repository.member.MemberRepository;
import com.fillin.service.searchHistory.SearchHistoryService;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.exception.BusinessException;
import com.fillin.global.apiPayload.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "검색", description = "검색 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;
    private final MemberRepository memberRepository;

    @Operation(summary = "최근 검색어 목록 조회")
    @GetMapping("/recent/list")
    public Response<RecentSearchListResponse> getRecentSearches(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) LocalDateTime cursorTime,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (size <= 0 || size > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if ((cursorTime == null) != (cursorId == null)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Slice<SearchHistory> slice =
                searchHistoryService.getRecentSearches(member, cursorTime, cursorId, size);

        List<SearchHistoryResponse> items = slice.getContent()
                .stream()
                .map(SearchHistoryResponse::from)
                .toList();

        return Response.ok(new RecentSearchListResponse(items, slice.hasNext()));
    }

    @Operation(summary = "최근 검색어 추가")
    @PostMapping("/recent")
    public Response<Void> saveSearchKeyword(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SearchHistoryCreateRequest request
    ) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        searchHistoryService.saveSearchKeyword(member, request.getKeyword().trim());

        return Response.ok();
    }

    @Operation(summary = "최근 검색어 삭제")
    @DeleteMapping("/recent/{id}")
    public Response<Void> deleteSearchHistory(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id
    ) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        searchHistoryService.deleteSearchHistory(member, id);

        return Response.ok();
    }

    @Operation(summary = "최근 검색어 전체 삭제")
    @DeleteMapping("/recent/all")
    public Response<Void> deleteAllSearchHistories(
            @AuthenticationPrincipal Long memberId
    ) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        searchHistoryService.deleteAllSearchHistory(member);

        return Response.ok();
    }
}

