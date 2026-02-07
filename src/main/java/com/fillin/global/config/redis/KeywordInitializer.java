package com.fillin.global.config.redis;

import com.fillin.domain.enums.KeywordCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("redis")
@RequiredArgsConstructor
public class KeywordInitializer implements ApplicationRunner {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(ApplicationArguments args) {

        for (KeywordCategory category : KeywordCategory.values()) {

            String keywordKey = "keywords:" + category.name();
            String popularKey = "popular:" + category.name();

            for (String keyword : category.getKeywords()) {
                try {
                    redisTemplate.opsForSet().add(keywordKey, keyword);

                    // ZSet에 score가 없는 경우만 추가
                    Double score = redisTemplate.opsForZSet().score(popularKey, keyword);
                    if (score == null) {
                        redisTemplate.opsForZSet().add(popularKey, keyword, 0);
                        log.info("Redis 초기화: '{}' 키워드 {} 카테고리 0점 추가", keyword, category.name());
                    }

                } catch (Exception e) {
                    log.error("Redis 초기화 중 오류 발생: {}", keyword, e);
                }
            }
        }
        log.info("Redis 키워드 초기화 완료");
    }
}