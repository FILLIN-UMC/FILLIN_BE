package com.fillin.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ValidType {
    VALID("현재 유효", "최근에도 확인됐어요"),
    UNCERTAIN("중간 상태", "제보 의견이 나뉘어요"),
    INVALID("안 유효", "오래된 제보일 수 있어요");

    private final String title;
    private final String description;
}
