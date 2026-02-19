package com.fillin.dto.report.response;

import com.fillin.domain.enums.ReportCategory;

public record MapMarkerResponse(
        Long id,
        Double latitude,
        Double longitude,
        ReportCategory category,
        String imageUrl
) {}