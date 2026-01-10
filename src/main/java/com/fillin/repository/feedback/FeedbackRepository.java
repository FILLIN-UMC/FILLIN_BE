package com.fillin.repository.feedback;

import com.fillin.domain.enums.FeedbackType;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository {
    int countByReportIdAndType(Long reportId, FeedbackType type);
}
