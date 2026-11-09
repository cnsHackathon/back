package com.cnsHackathon.cns.session.profile;

import com.cnsHackathon.cns.global.support.Labeled;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum AgeGroup implements Labeled {

    TEENS("10대"),
    TWENTIES("20대"),
    THIRTIES("30대"),
    FORTIES("40대"),
    FIFTIES("50대"),
    SIXTIES("60대"),
    SEVENTIES_PLUS("70대 이상");

    private final String label;

    AgeGroup(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    @JsonCreator
    public static AgeGroup from(String value) {
        return Labeled.parse(AgeGroup.class, value);
    }
}
