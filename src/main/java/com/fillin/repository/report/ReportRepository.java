package com.fillin.repository.report;

import com.fillin.domain.Report;
import com.fillin.domain.enums.ReportCategory;
import com.fillin.domain.enums.ReportStatus;
import jakarta.persistence.Id;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

        List<Report> findByTitleContainingAndLatitudeBetweenAndLongitudeBetween(
                String title,
                Double minLatitude,
                Double maxLatitude,
                Double minLongitude,
                Double maxLongitude
        );

        //만료 기간 지난 제보들 status 변경
        @Modifying
        @Query("UPDATE Report r SET r.status = com.fillin.domain.enums.ReportStatus.EXPIRED " +
                "WHERE r.expiresAt <= :now AND r.status != com.fillin.domain.enums.ReportStatus.EXPIRED")
        int updateStatusToExpired(@Param("now") LocalDateTime now);

        //게시 2주 지난 제보들 valid타입 변경
        @Modifying
        @Query("UPDATE Report r SET r.validType = com.fillin.domain.enums.ValidType.INVALID " +
                "WHERE r.createdAt <= :threshold AND r.validType != com.fillin.domain.enums.ValidType.INVALID")
        int updateValidTypeToInvalid(@Param("threshold") LocalDateTime threshold);

        // 1. 사라지는 기준: 최근 7일 동안 부정 의견(DONE)이 3건 이상인 제보 조회
        @Query("SELECT r FROM Report r WHERE r.status = com.fillin.domain.enums.ReportStatus.PUBLISHED " +
                "AND (SELECT COUNT(f) FROM Feedback f WHERE f.report = r " +
                "     AND f.type = com.fillin.domain.enums.FeedbackType.DONE " +
                "     AND f.createdAt >= :sevenDaysAgo) >= 3")
        List<Report> findReportsWithHighNegativeFeedback(@Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);

        // 2. 안 유효 기준: 등록한 지 2주 이상 된 제보 INVALID로 벌크 업데이트
        @Modifying
        @Query("UPDATE Report r SET r.validType = com.fillin.domain.enums.ValidType.INVALID " +
                "WHERE r.createdAt <= :twoWeeksAgo AND r.status = com.fillin.domain.enums.ReportStatus.PUBLISHED")
        int updateOldReportsToInvalid(@Param("twoWeeksAgo") LocalDateTime twoWeeksAgo);

        // 3. 비율 판단용: 상태가 PUBLISHED이고 2주가 지나지 않은 제보들 조회
        @Query("SELECT r FROM Report r WHERE r.status = com.fillin.domain.enums.ReportStatus.PUBLISHED " +
                "AND r.createdAt > :twoWeeksAgo AND r.createdAt <= :threeDaysAgo")
        List<Report> findActiveReportsForRatioCheck(@Param("twoWeeksAgo") LocalDateTime twoWeeksAgo,
                                                    @Param("threeDaysAgo") LocalDateTime threeDaysAgo);

        List<Report> findByMemberIdAndStatus(Long memberId, ReportStatus status);

        @Query("SELECT r FROM Report r WHERE r.status = com.fillin.domain.enums.ReportStatus.PUBLISHED " +
                "AND r.createdAt > :twoWeeksAgo")
        List<Report> findActiveReportsForValidation(@Param("twoWeeksAgo") LocalDateTime twoWeeksAgo);

        // 내 주변 인기장소 발견 관련 조회 내 주변 3km 이내로 설정 좋아요 순, 상위 6개
        @Query(value = """
        SELECT * FROM report r 
        WHERE ST_Distance_Sphere(POINT(r.longitude, r.latitude), POINT(:lon, :lat)) <= 3000
        AND r.status = 'PUBLISHED'
        ORDER BY r.like_count DESC, 
                 ST_Distance_Sphere(POINT(r.longitude, r.latitude), POINT(:lon, :lat)) ASC
        """, nativeQuery = true)
        List<Report> findHotReports(
                @Param("lat") double lat,
                @Param("lon") double lon,
                Pageable pageable
        );

    // 지도 영역 내의 모든 '게시된' 제보를 조회하는 메서드
    List<Report> findByLatitudeBetweenAndLongitudeBetweenAndStatus(
            Double minLatitude, Double maxLatitude,
            Double minLongitude, Double maxLongitude,
            ReportStatus status
    );
}
