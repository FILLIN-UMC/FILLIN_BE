package com.fillin.domain.enums.rank;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Tamheomga {
    TAMHEOMGA_0("탐험가 0",0),
    TAMHEOMGA_1("탐험가 1",5),
    TAMHEOMGA_2("탐험가 2",10),
    TAMHEOMGA_3("탐험가 3",15),
    TAMHEOMGA_4("탐험가 4",20),
    TAMHEOMGA_5("탐험가 5",25);

    @JsonValue
    private final String displayName;
    private final int minReport;

    Tamheomga(String displayName, int minReport) {
        this.displayName = displayName;
        this.minReport = minReport;
    }
}
