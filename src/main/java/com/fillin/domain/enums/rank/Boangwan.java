package com.fillin.domain.enums.rank;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Boangwan {
    BOANGWAN_0("보안관 0",0),
    BOANGWAN_1("보안관 1",5),
    BOANGWAN_2("보안관 2",10),
    BOANGWAN_3("보안관 3",15),
    BOANGWAN_4("보안관 4",20),
    BOANGWAN_5("보안관 5",25);

    @JsonValue
    private final String displayName;
    private final int minReport;

    Boangwan(String displayName, int minReport) {
        this.displayName = displayName;
        this.minReport = minReport;
    }
}
