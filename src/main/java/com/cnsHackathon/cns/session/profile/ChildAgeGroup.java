package com.cnsHackathon.cns.session.profile;

import com.cnsHackathon.cns.global.support.Labeled;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum ChildAgeGroup implements Labeled {

    PRESCHOOL("미취학"),
    ELEMENTARY("초등학생"),
    SECONDARY("중고등학생"),
    ADULT("성인");

    private final String label;

    ChildAgeGroup(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    @JsonCreator
    public static ChildAgeGroup from(String value) {
        return Labeled.parse(ChildAgeGroup.class, value);
    }
}
