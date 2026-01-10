package com.fillin.repository.feedback;

import com.fillin.domain.Feedback;
import com.fillin.domain.enums.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    int countByReportIdAndType(Long reportId, FeedbackType type);
}
