package com.fillin.domain.searchHistory.repository;

import com.fillin.domain.Member;
import com.fillin.domain.searchHistory.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    // 첫 페이지
    Slice<SearchHistory> findByMemberOrderBySearchAtDescIdDesc(
            Member member,
            Pageable pageable);

    // 다음 페이지부터
    @Query("""
                SELECT s
                FROM SearchHistory s
                WHERE s.member = :member
                    AND (
                            s.searchAt < :cursorTime
                        OR (s.searchAt = :cursorTime AND s.id < :cursorId)
                    )
                ORDER BY s.searchAt DESC, s.id DESC
            """)
    Slice<SearchHistory> findNextPage(
            @Param("member") Member member,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    Optional<SearchHistory> findByMemberAndKeyword(
            Member member, String keyword);

}
