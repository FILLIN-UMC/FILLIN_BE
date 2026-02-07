package com.fillin.dto.report.response;

public record ReportImageProcessResponse(
        boolean hasLicensePlate,     // 번호판 감지 여부
        String processedImageUrl     // 처리된 이미지 URL (감지 안 되면 null)
) {
    // 팩토리 메서드: 번호판 없음 (빠른 반환)
    public static ReportImageProcessResponse notDetected() {
        return new ReportImageProcessResponse(false, null);
    }

    // 팩토리 메서드: 번호판 있음 (모자이크 완료 후 URL 반환)
    public static ReportImageProcessResponse detected(String imageUrl) {
        return new ReportImageProcessResponse(true, imageUrl);
    }
}