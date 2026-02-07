package com.fillin.service.report;

import com.fillin.dto.report.response.AutoCompleteItemResponse;
import com.fillin.dto.report.response.AutoCompleteResponse;
import com.fillin.global.apiPayload.code.ErrorCode;
import com.fillin.global.apiPayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AutoCompleteService {

    private final StringRedisTemplate redisTemplate;
    private static final int LIMIT = 5;

    public AutoCompleteResponse autocomplete(String query) {

        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.AUTOCOMPLETE_INVALID_QUERY);
        }

        try {
            List<AutoCompleteItemResponse> result;

            // 카테고리 단어 검색
            if (isCategoryKeyword(query)) {
                result = searchByCategory(query);
            } else {
                result = searchByContains(query);
            }

            if (result.isEmpty()) {
                return new AutoCompleteResponse(
                        "검색 결과가 없습니다.",
                        List.of()
                );
            }

            return new AutoCompleteResponse(
                    "자동완성 결과입니다.",
                    result
            );

        } catch (BusinessException e) {
            // 이미 의도된 예외는 그대로 던짐
            throw e;
        } catch (Exception e) {
            // Redis 장애, 타임아웃, 연결 실패 등
            throw new BusinessException(
                    ErrorCode.AUTOCOMPLETE_REDIS_ERROR
            );
        }
    }

    private boolean isCategoryKeyword(String query) {
        return query.equals("위험")
                || query.equals("경고")
                || query.equals("발견");
    }

    private List<AutoCompleteItemResponse> searchByCategory(String query) {

        String category = switch (query) {
            case "위험" -> "DANGER";
            case "경고" -> "INCONVENIENCE";
            case "발견" -> "DISCOVERY";
            default -> throw new BusinessException(
                    ErrorCode.AUTOCOMPLETE_CATEGORY_NOT_FOUND
            );
        };

        Set<String> keywords = redisTemplate.opsForZSet()
                .reverseRange("popular:" + category, 0, LIMIT - 1);

        if (keywords == null) return List.of();

        return keywords.stream()
                .map(k -> new AutoCompleteItemResponse(k, category))
                .toList();
    }

    private List<AutoCompleteItemResponse> searchByContains(String query) {

        return List.of("DANGER", "INCONVENIENCE", "DISCOVERY")
                .stream()
                .flatMap(category -> {
                    Set<String> keywords =
                            redisTemplate.opsForSet()
                                    .members("keywords:" + category);

                    if (keywords == null) return Stream.empty();

                    return keywords.stream()
                            .filter(k -> k.contains(query))
                            .map(k -> new AutoCompleteItemResponse(k, category));
                })
                .distinct()
                .limit(LIMIT)
                .toList();
    }
}
