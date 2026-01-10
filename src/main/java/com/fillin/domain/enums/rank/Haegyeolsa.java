package com.fillin.domain.enums.rank;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Haegyeolsa {
    HAEGYEOLSA_0("해결사 0",0),
    HAEGYEOLSA_1("해결사 1",5),
    HAEGYEOLSA_2("해결사 2",10),
    HAEGYEOLSA_3("해결사 3",15),
    HAEGYEOLSA_4("해결사 4",20),
    HAEGYEOLSA_5("해결사 5",25);

    @JsonValue
    private final String displayName;
    private final int minReport;

    Haegyeolsa(String displayName, int minReport) {
        this.displayName = displayName;
        this.minReport = minReport;
    }
}
