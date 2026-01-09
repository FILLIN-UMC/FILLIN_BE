package com.fillin.repository.report;

import com.fillin.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    int countByMemberId(Long memberId);

    @Query("SELECT COALESCE(SUM(r.viewCount), 0) FROM Report r WHERE r.member.id = :memberId")
    int totalViewCountByMemberId(@Param("memberId") Long memberId);
}
