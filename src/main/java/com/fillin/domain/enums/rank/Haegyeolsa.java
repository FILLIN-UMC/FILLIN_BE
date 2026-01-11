package com.fillin.domain.enums.rank;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
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

    public static Haegyeolsa resolveHaegyeolsaRank(int count) {
        for (int i = Haegyeolsa.values().length - 1; i >= 0; i--) {
            Haegyeolsa rank = Haegyeolsa.values()[i];
            if (count >= rank.getMinReport()) { // Boangwan에 getter 필요
                return rank;
            }
        }
        return Haegyeolsa.HAEGYEOLSA_0;
    }

    public static int getNextTarget(int currentCount) {
        for (Haegyeolsa rank : values()) {
            if (rank.minReport > currentCount) return rank.minReport;
        }
        return HAEGYEOLSA_5.minReport; // 만렙 시 마지막 기준 유지
    }
}
