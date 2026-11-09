package com.cnsHackathon.cns.session.profile;

import com.cnsHackathon.cns.global.support.Texts;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** 연습용 프로필. 연령대·성별·직업은 필수, 나머지는 입력한 경우에만 값이 있다. */
public record Profile(
        AgeGroup ageGroup,
        Gender gender,
        Job job,
        Boolean married,
        List<Child> children,
        String region) {

    private static final String NOT_GIVEN = "입력 안 함";

    public Profile {
        children = children == null ? List.of() : List.copyOf(children);
        region = Texts.trimToNull(region);
    }

    public boolean isMarried() {
        return Boolean.TRUE.equals(married);
    }

    public boolean hasChildIn(Collection<ChildAgeGroup> ageGroups) {
        return children.stream().anyMatch(child -> ageGroups.contains(child.ageGroup()));
    }

    /** 프롬프트에 넣는 형태. 입력하지 않은 항목도 적어서 AI가 지어내지 않게 한다. */
    public String describe() {
        String marriedText = married == null ? NOT_GIVEN : married ? "기혼" : "미혼";
        String childrenText = children.isEmpty() ? NOT_GIVEN
                : children.stream().map(Child::describe).collect(Collectors.joining(", "));
        return String.join("\n",
                "- 연령대: " + ageGroup.label(),
                "- 성별: " + gender.label(),
                "- 직업: " + job.label(),
                "- 기혼 여부: " + marriedText,
                "- 자녀: " + childrenText,
                "- 거주지: " + (region == null ? NOT_GIVEN : region));
    }

    public record Child(Gender gender, ChildAgeGroup ageGroup) {

        String describe() {
            return gender.childLabel() + "(" + ageGroup.label() + ")";
        }
    }
}
