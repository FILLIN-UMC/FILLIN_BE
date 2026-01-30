package com.fillin.dto.report.response;

import com.fillin.domain.Report;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotReportResponse {
    private Long reportId;      // 제보 ID (상세 페이지 이동용)
    private String title;       // 제보 제목
    private String imageUrl;    // 제보 이미지 URL
    private int likeCount;      // 좋아요 개수
    private String address;     // 주소 (텍스트)
    private Double latitude;    // 지도 마커 이동용 위도
    private Double longitude;   // 지도 마커 이동용 경도

    // Entity -> DTO 변환 메서드 (Factory Method)
    public static HotReportResponse from(Report report) {
        return HotReportResponse.builder()
                .reportId(report.getId())
                .title(report.getTitle())
                .imageUrl(report.getReportImageUrl()) // 엔티티 필드명: reportImageUrl
                .likeCount(report.getLikeCount())
                .address(report.getAddress())
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .build();
    }
}