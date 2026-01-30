package com.fillin.domain.enums.rank;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Boangwan {
    BOANGWAN_0("보안관 0",0),
    BOANGWAN_1("보안관 1",5),
    BOANGWAN_2("보안관 2",10),
    BOANGWAN_3("보안관 3",15),
    BOANGWAN_4("보안관 4",20),
    BOANGWAN_5("보안관 5",25);

    @JsonValue
    private final String displayName;
    @Getter
    private final int minReport;

    Boangwan(String displayName, int minReport) {
        this.displayName = displayName;
        this.minReport = minReport;
    }

    public static Boangwan resolveBoangwanRank(int count) {
        Boangwan[] ranks = values();
        for (int i = ranks.length - 1; i >= 0; i--) {
            if (count >= ranks[i].minReport) {
                return ranks[i];
            }
        }
        return BOANGWAN_0; // 기본값
    }

    public static int getNextTarget(int currentCount) {
        for (Boangwan rank : values()) {
            if (rank.minReport > currentCount) return rank.minReport;
        }
        return BOANGWAN_5.minReport; // 만렙 시 마지막 기준 유지
    }
}
