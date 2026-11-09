package com.cnsHackathon.cns.session.domain;

import com.cnsHackathon.cns.session.profile.ChildAgeGroup;
import com.cnsHackathon.cns.session.profile.Gender;
import com.cnsHackathon.cns.session.profile.Profile;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class ChildInfo {

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 20)
    private ChildAgeGroup ageGroup;

    protected ChildInfo() {
    }

    private ChildInfo(Gender gender, ChildAgeGroup ageGroup) {
        this.gender = gender;
        this.ageGroup = ageGroup;
    }

    static ChildInfo from(Profile.Child child) {
        return new ChildInfo(child.gender(), child.ageGroup());
    }

    Profile.Child toChild() {
        return new Profile.Child(gender, ageGroup);
    }
}
