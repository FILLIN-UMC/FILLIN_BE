package com.fillin.global.config.redis;

import com.fillin.domain.enums.KeywordCategory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KeywordInitializer {

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        for (KeywordCategory category : KeywordCategory.values()) {

            String keywordKey = "keywords:" + category.name();
            String popularKey = "popular:" + category.name();

            for (String keyword : category.getKeywords()) {

                // 글자 검색용
                redisTemplate.opsForSet().add(keywordKey, keyword);

                // 인기 키워드용 (임시 점수 0)
                redisTemplate.opsForZSet().add(popularKey, keyword, 0);
            }
        }
    }
}

