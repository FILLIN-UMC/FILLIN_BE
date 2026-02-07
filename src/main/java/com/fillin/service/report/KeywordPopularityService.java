package com.fillin.service.report;

import com.fillin.domain.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeywordPopularityService {

    private final StringRedisTemplate redisTemplate;

    public void increase(Report report) {

        if (report.getKeyword() == null || report.getKeyword().isBlank()) return;

        try {
            redisTemplate.opsForZSet()
                    .incrementScore(
                            "popular:" + report.getCategory().name(),
                            report.getKeyword(),
                            1
                    );
        } catch (Exception e) {
            log.warn("Redis keyword popularity update failed", e);
        }
    }
}
