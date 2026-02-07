package com.fillin.dto.report.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;


@Getter
@AllArgsConstructor
public class AutoCompleteResponse {

    private String message;
    private List<AutoCompleteItemResponse> items;
}
