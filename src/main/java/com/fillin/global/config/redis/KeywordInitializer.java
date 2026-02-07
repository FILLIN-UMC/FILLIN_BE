package com.fillin.global.config.redis;

import com.fillin.domain.enums.KeywordCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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

                redisTemplate.opsForSet().add(keywordKey, keyword);

                Double score = redisTemplate.opsForZSet()
                        .score(popularKey, keyword);

                if (score == null) {
                    redisTemplate.opsForZSet()
                            .add(popularKey, keyword, 0);
                }
            }
        }
    }
}
