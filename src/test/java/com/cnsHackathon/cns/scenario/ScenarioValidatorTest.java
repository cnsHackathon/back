package com.cnsHackathon.cns.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioValidatorTest {

    @Test
    void 올바른_시나리오는_문제가_없다() {
        assertEquals(List.of(), ScenarioValidator.problems(valid(
                List.of(new Scenario.Persona("C", "사기범"), new Scenario.Persona("B", "공범")),
                List.of(new Scenario.Checkpoint("DOUBT", "의심", 40), new Scenario.Checkpoint("REFUSE", "거절", 60)))));
    }

    @Test
    void 가중치_합이_100이_아니면_알려_준다() {
        List<String> problems = ScenarioValidator.problems(valid(
                List.of(new Scenario.Persona("C", "사기범")),
                List.of(new Scenario.Checkpoint("DOUBT", "의심", 40))));

        assertTrue(problems.stream().anyMatch(problem -> problem.contains("100")), problems.toString());
    }

    @Test
    void 등장인물은_C로_시작하고_C와_B만_쓸_수_있다() {
        List<String> problems = ScenarioValidator.problems(valid(
                List.of(new Scenario.Persona("B", "공범"), new Scenario.Persona("D", "세 번째")),
                List.of(new Scenario.Checkpoint("REFUSE", "거절", 100))));

        assertTrue(problems.stream().anyMatch(problem -> problem.contains("C여야")), problems.toString());
        assertTrue(problems.stream().anyMatch(problem -> problem.contains(": D")), problems.toString());
    }

    private static Scenario valid(List<Scenario.Persona> personas, List<Scenario.Checkpoint> checkpoints) {
        return new Scenario("sample-01", ScamType.LOAN, "제목", null, personas,
                List.of("흐름"), "요구", List.of(), checkpoints, List.of(), List.of());
    }
}
