package com.fillin.dto.report.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AutoCompleteItemResponse {

    private String keyword; // 자동완성 키워드
    private String category; // 어떤 카테고리인지
}
