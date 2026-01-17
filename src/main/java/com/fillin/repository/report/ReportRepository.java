package com.fillin.repository.report;

import com.fillin.domain.Report;
import com.fillin.domain.enums.ReportCategory;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
        int countByMemberId(Long memberId);

        @Query("SELECT COALESCE(SUM(r.viewCount), 0) FROM Report r WHERE r.member.id = :memberId")
        int totalViewCountByMemberId(@Param("memberId") Long memberId);

        int countByMemberIdAndCategory(Long memberId, ReportCategory category);

        int countByMemberIdAndCategoryAndExpiresAtBetween(
                        Long memberId,
                        ReportCategory category,
                        LocalDateTime start,
                        LocalDateTime end);

        List<Report> findByMemberIdAndExpiresAtBetween(
                        @Param("memberId") Long memberId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        List<Report> findByExpiresAtBetween(LocalDateTime start, LocalDateTime end);

        // 만료된 제보들 (현재 시간보다 이전인 경우)
        List<Report> findByMemberIdAndExpiresAtBefore(Long memberId, LocalDateTime now);

        // 만료되지 않은 제보들 (현재 시간보다 이후인 경우)
        List<Report> findByMemberIdAndExpiresAtAfter(Long memberId, LocalDateTime now);

        // 특정 사용자가 좋아요한 제보들
        @Query("SELECT l.report FROM Like l WHERE l.member.id = :memberId")
        List<Report> findAllByMemberIdInLikes(@Param("memberId") Long memberId);

        List<Report> findTop6ByCategoryInOrderByLikeCountDescCreatedAtDesc(List<ReportCategory> reportCategories);

        List<Report> findByKeywordAndLatitudeBetweenAndLongitudeBetween(
                String keyword,
                Double minLatitude,
                Double maxLatitude,
                Double minLongitude,
                Double maxLongitude
        );
}
