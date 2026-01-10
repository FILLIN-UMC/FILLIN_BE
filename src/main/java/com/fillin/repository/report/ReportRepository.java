package com.fillin.repository.report;

import com.fillin.domain.Report;
import com.fillin.domain.enums.ReportCategory;
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
            LocalDateTime end
    );

    List<Report> findByMemberIdAndExpiresAtBetween(
            @Param("memberId") Long memberId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
