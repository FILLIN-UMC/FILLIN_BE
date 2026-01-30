package com.fillin.repository.alarm;

import com.fillin.domain.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

      // 전체 알림 (3개월 이내)
      @Query("""
              SELECT a
              FROM Alarm a
              WHERE a.member.id = :memberId
                AND a.createdAt >= :threeMonthsAgo
              ORDER BY a.createdAt DESC
          """)
      List<Alarm> findRecentAlarms(
          @Param("memberId") Long memberId,
          @Param("threeMonthsAgo") LocalDateTime threeMonthsAgo);

      // 읽음/안읽음 필터
      @Query("""
              SELECT a
              FROM Alarm a
              WHERE a.member.id = :memberId
                AND a.isRead = :read
                AND a.createdAt >= :threeMonthsAgo
              ORDER BY a.createdAt DESC
          """)
      List<Alarm> findRecentAlarmsByRead(
          @Param("memberId") Long memberId,
          @Param("read") boolean read,
          @Param("threeMonthsAgo") LocalDateTime threeMonthsAgo);

      Optional<Alarm> findByIdAndMemberId(Long id, Long memberId);

      void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
