package com.cnsHackathon.cns.scenario;

import com.cnsHackathon.cns.global.support.Texts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** 시나리오 JSON을 서버 시작 시점에 검사한다. 잘못된 파일이 있으면 서버가 뜨지 않고 이유를 알려 준다. */
final class ScenarioValidator {

    static final List<String> SPEAKER_KEYS = List.of("C", "B");
    private static final Pattern ID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern CHECKPOINT_ID = Pattern.compile("[A-Z][A-Z0-9_]*");

    private ScenarioValidator() {
    }

    static List<String> problems(Scenario scenario) {
        List<String> problems = new ArrayList<>();
        if (!Texts.hasText(scenario.id()) || !ID.matcher(scenario.id()).matches()) {
            problems.add("id는 소문자, 숫자, 하이픈으로 써 주세요. (예: noshow-military-01)");
        }
        if (scenario.subType() == null) {
            problems.add("subType이 없어요.");
        }
        if (!Texts.hasText(scenario.title())) {
            problems.add("title이 없어요.");
        }
        checkPersonas(scenario, problems);
        if (scenario.flow().isEmpty()) {
            problems.add("flow가 비어 있어요.");
        }
        if (!Texts.hasText(scenario.demand())) {
            problems.add("demand가 없어요.");
        }
        checkCheckpoints(scenario, problems);
        return problems;
    }

    private static void checkPersonas(Scenario scenario, List<String> problems) {
        List<Scenario.Persona> personas = scenario.personas();
        if (personas.isEmpty() || personas.size() > SPEAKER_KEYS.size()) {
            problems.add("personas는 1~2명이어야 해요. 프론트가 C, B 두 목소리만 구분해요.");
            return;
        }
        if (!"C".equals(personas.get(0).key())) {
            problems.add("첫 번째 등장인물의 key는 C여야 해요.");
        }
        Set<String> seen = new HashSet<>();
        for (Scenario.Persona persona : personas) {
            if (!SPEAKER_KEYS.contains(persona.key()) || !seen.add(persona.key())) {
                problems.add("등장인물 key는 C, B 중 하나이고 겹치면 안 돼요: " + persona.key());
            }
            if (!Texts.hasText(persona.role())) {
                problems.add("등장인물 " + persona.key() + "의 role이 없어요.");
            }
        }
    }

    private static void checkCheckpoints(Scenario scenario, List<String> problems) {
        if (scenario.checkpoints().isEmpty()) {
            problems.add("checkpoints가 비어 있어요.");
            return;
        }
        Set<String> seen = new HashSet<>();
        int totalWeight = 0;
        for (Scenario.Checkpoint checkpoint : scenario.checkpoints()) {
            if (!Texts.hasText(checkpoint.id()) || !CHECKPOINT_ID.matcher(checkpoint.id()).matches()) {
                problems.add("체크포인트 id는 대문자와 밑줄로 써 주세요: " + checkpoint.id());
            } else if (!seen.add(checkpoint.id())) {
                problems.add("체크포인트 id가 겹쳐요: " + checkpoint.id());
            }
            if (!Texts.hasText(checkpoint.desc())) {
                problems.add("체크포인트 " + checkpoint.id() + "의 desc가 없어요.");
            }
            if (checkpoint.weight() == null || checkpoint.weight() <= 0) {
                problems.add("체크포인트 " + checkpoint.id() + "의 weight는 0보다 커야 해요.");
            } else {
                totalWeight += checkpoint.weight();
            }
        }
        if (totalWeight != 100) {
            problems.add("체크포인트 weight 합이 100이어야 해요. 지금은 " + totalWeight + "이에요.");
        }
    }
}
