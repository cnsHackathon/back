package com.cnsHackathon.cns.session.dto;

import com.cnsHackathon.cns.session.profile.AgeGroup;
import com.cnsHackathon.cns.session.profile.ChildAgeGroup;
import com.cnsHackathon.cns.session.profile.Gender;
import com.cnsHackathon.cns.session.profile.Job;
import com.cnsHackathon.cns.session.profile.Profile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 예: { "ageGroup": "30대", "gender": "FEMALE", "job": "자영업자", "married": true,
 *       "children": [{ "gender": "MALE", "ageGroup": "초등학생" }], "region": "서울 마포구" }
 *
 * @param scenarioId 개발·시연용. 넣으면 랜덤 배정 대신 그 시나리오로 통화한다. 프론트는 보내지 않는다.
 */
public record CreateSessionRequest(
        @NotNull AgeGroup ageGroup,
        @NotNull Gender gender,
        @NotNull Job job,
        Boolean married,
        @Size(max = 10) List<@NotNull @Valid ChildRequest> children,
        @Size(max = 50) String region,
        @Size(max = 100) String scenarioId) {

    public Profile toProfile() {
        List<Profile.Child> kids = children == null ? List.of()
                : children.stream().map(child -> new Profile.Child(child.gender(), child.ageGroup())).toList();
        return new Profile(ageGroup, gender, job, married, kids, region);
    }

    public record ChildRequest(@NotNull Gender gender, @NotNull ChildAgeGroup ageGroup) {
    }
}
