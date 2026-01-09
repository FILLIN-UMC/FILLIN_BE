package com.fillin.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Rank {
    BOANGWAN_0("보안관 0",0),
    BOANGWAN_1("보안관 1",5),
    BOANGWAN_2("보안관 2",10),
    BOANGWAN_3("보안관 3",15),
    BOANGWAN_4("보안관 4",20),
    BOANGWAN_5("보안관 5",25),

    HAEGYEOLSA_0("해결사 0",0),
    HAEGYEOLSA_1("해결사 1",5),
    HAEGYEOLSA_2("해결사 2",10),
    HAEGYEOLSA_3("해결사 3",15),
    HAEGYEOLSA_4("해결사 4",20),
    HAEGYEOLSA_5("해결사 5",25),

    TAMHEOMGA_0("탐험가 0",0),
    TAMHEOMGA_1("탐험가 1",5),
    TAMHEOMGA_2("탐험가 2",10),
    TAMHEOMGA_3("탐험가 3",15),
    TAMHEOMGA_4("탐험가 4",20),
    TAMHEOMGA_5("탐험가 5",25);

    @JsonValue
    private final String displayName;
    private final int minReport;

    Rank(String displayName, int minReport) {
        this.displayName = displayName;
        this.minReport = minReport;
    }
}


