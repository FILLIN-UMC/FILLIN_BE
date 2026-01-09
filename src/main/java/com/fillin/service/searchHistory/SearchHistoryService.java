package com.fillin.service.searchHistory;

import com.fillin.domain.Member;
import com.fillin.domain.SearchHistory;
import com.fillin.repository.searchHistory.SearchHistoryRepository;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    public Slice<SearchHistory> getRecentSearches(
            Member member,
            LocalDateTime cursorTime,
            Long cursorId,
            int size) {
        Pageable pageable = PageRequest.of(0, size);

        if (cursorTime == null && cursorId == null) {
            return searchHistoryRepository.findByMemberOrderBySearchAtDescIdDesc(member, pageable);
        } else {
            return searchHistoryRepository.findNextPage(member, cursorTime, cursorId, pageable);
        }
    }

    @Transactional
    public void saveSearchKeyword(Member member, String keyword) {
        if (keyword.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        searchHistoryRepository.findByMemberAndKeyword(member, keyword)
                .ifPresentOrElse(
                        history -> history.updateSearchAt(LocalDateTime.now()),
                        () -> searchHistoryRepository.save(
                                SearchHistory.builder()
                                        .member(member)
                                        .keyword(keyword)
                                        .searchAt(LocalDateTime.now())
                                        .build()
                        )

                );
    }

    public void deleteSearchHistory(Member member, Long searchHistoryId) {
        SearchHistory history = searchHistoryRepository
                .findByIdAndMember(searchHistoryId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        searchHistoryRepository.delete(history);
    }

    public void deleteAllSearchHistory(Member member) {
        searchHistoryRepository.deleteAllByMember(member);
    }
}
