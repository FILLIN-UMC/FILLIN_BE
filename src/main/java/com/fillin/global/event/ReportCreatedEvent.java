package com.fillin.global.event;

import com.fillin.domain.enums.ReportCategory;

public record ReportCreatedEvent(
        Long memberId,
        ReportCategory category
) {}
