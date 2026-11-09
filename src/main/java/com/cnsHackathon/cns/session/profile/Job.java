package com.cnsHackathon.cns.session.profile;

import com.cnsHackathon.cns.global.support.Labeled;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum Job implements Labeled {

    STUDENT("학생"),
    EMPLOYEE("직장인"),
    SELF_EMPLOYED("자영업자"),
    HOMEMAKER("주부"),
    UNEMPLOYED_OR_RETIRED("무직·은퇴"),
    OTHER("기타");

    private final String label;

    Job(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    @JsonCreator
    public static Job from(String value) {
        return Labeled.parse(Job.class, value);
    }
}
