package com.cnsHackathon.cns.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnsHackathon.cns.session.profile.AgeGroup;
import com.cnsHackathon.cns.session.profile.ChildAgeGroup;
import com.cnsHackathon.cns.session.profile.Gender;
import com.cnsHackathon.cns.session.profile.Job;
import com.cnsHackathon.cns.session.profile.Profile;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScenarioSelectorTest {

    private static final List<Scenario> ALL = List.of(
            scenario("institution-1", ScamType.INSTITUTION, null),
            scenario("institution-2", ScamType.INSTITUTION, null),
            scenario("institution-3", ScamType.INSTITUTION, null),
            scenario("loan-1", ScamType.LOAN, null),
            scenario("kidnap-1", ScamType.FAMILY_KIDNAP,
                    new Scenario.Eligibility(null, false, List.of(ChildAgeGroup.ELEMENTARY, ChildAgeGroup.SECONDARY))),
            scenario("child-1", ScamType.FAMILY_CHILD,
                    new Scenario.Eligibility(null, false, List.of(ChildAgeGroup.SECONDARY, ChildAgeGroup.ADULT))),
            scenario("spouse-1", ScamType.FAMILY_SPOUSE, new Scenario.Eligibility(null, true, null)),
            scenario("noshow-1", ScamType.NOSHOW_PROXY_PURCHASE,
                    new Scenario.Eligibility(List.of(Job.SELF_EMPLOYED), false, null)));

    @Test
    void 선택_입력이_없으면_조건_없는_유형만_후보가_된다() {
        Profile profile = new Profile(AgeGroup.TWENTIES, Gender.MALE, Job.STUDENT, null, null, null);

        assertEquals(Set.of(ScamType.INSTITUTION, ScamType.LOAN), typesOf(ScenarioSelector.candidates(ALL, profile)));
    }

    @Test
    void 자영업자이고_기혼에_성인_자녀가_있으면_해당_유형이_모두_열린다() {
        Profile profile = new Profile(AgeGroup.FIFTIES, Gender.FEMALE, Job.SELF_EMPLOYED, true,
                List.of(new Profile.Child(Gender.MALE, ChildAgeGroup.ADULT)), "서울 마포구");

        assertEquals(
                Set.of(ScamType.INSTITUTION, ScamType.LOAN, ScamType.FAMILY_CHILD,
                        ScamType.FAMILY_SPOUSE, ScamType.NOSHOW_PROXY_PURCHASE),
                typesOf(ScenarioSelector.candidates(ALL, profile)));
    }

    @Test
    void 미취학_자녀만_있으면_자녀_사칭과_납치_빙자는_나오지_않는다() {
        Profile profile = new Profile(AgeGroup.THIRTIES, Gender.FEMALE, Job.EMPLOYEE, false,
                List.of(new Profile.Child(Gender.FEMALE, ChildAgeGroup.PRESCHOOL)), null);

        assertEquals(Set.of(ScamType.INSTITUTION, ScamType.LOAN), typesOf(ScenarioSelector.candidates(ALL, profile)));
    }

    @Test
    void 시나리오_수와_상관없이_유형이_고르게_뽑힌다() {
        Profile profile = new Profile(AgeGroup.TWENTIES, Gender.MALE, Job.STUDENT, null, null, null);
        List<Scenario> candidates = ScenarioSelector.candidates(ALL, profile);
        Random random = new Random(42);

        Map<ScamType, Integer> counts = new EnumMap<>(ScamType.class);
        for (int i = 0; i < 2000; i++) {
            counts.merge(ScenarioSelector.pick(candidates, random).subType(), 1, Integer::sum);
        }

        // 기관 사칭 시나리오가 3개, 대출 빙자가 1개여도 유형은 반반에 가깝게 나온다.
        int institution = counts.get(ScamType.INSTITUTION);
        int loan = counts.get(ScamType.LOAN);
        assertTrue(Math.abs(institution - loan) < 200, "institution=" + institution + ", loan=" + loan);
    }

    private static Scenario scenario(String id, ScamType type, Scenario.Eligibility eligibility) {
        return new Scenario(id, type, id, eligibility,
                List.of(new Scenario.Persona("C", "사기범")), List.of("흐름"), "요구", List.of(),
                List.of(new Scenario.Checkpoint("REFUSE", "거절", 100)), List.of(), List.of());
    }

    private static Set<ScamType> typesOf(List<Scenario> scenarios) {
        return scenarios.stream().map(Scenario::subType).collect(Collectors.toSet());
    }
}
