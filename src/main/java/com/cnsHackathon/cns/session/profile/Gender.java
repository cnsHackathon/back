package com.cnsHackathon.cns.session.profile;

import com.cnsHackathon.cns.global.support.Labeled;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender implements Labeled {

    MALE("남성", "아들"),
    FEMALE("여성", "딸");

    private final String label;
    private final String childLabel;

    Gender(String label, String childLabel) {
        this.label = label;
        this.childLabel = childLabel;
    }

    @Override
    public String label() {
        return label;
    }

    public String childLabel() {
        return childLabel;
    }

    @JsonCreator
    public static Gender from(String value) {
        return Labeled.parse(Gender.class, value);
    }
}
