package com.cnsHackathon.cns.scenario;

import com.cnsHackathon.cns.session.profile.Profile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * 프로필로 후보를 거른 뒤 랜덤으로 배정한다.
 * 유형별 시나리오 수가 달라도 유형이 고르게 나오도록 유형을 먼저 뽑고, 그 안에서 시나리오를 뽑는다.
 */
@Component
public class ScenarioSelector {

    private final ScenarioCatalog catalog;

    public ScenarioSelector(ScenarioCatalog catalog) {
        this.catalog = catalog;
    }

    public Scenario select(Profile profile) {
        return pick(candidates(catalog.all(), profile), ThreadLocalRandom.current());
    }

    static List<Scenario> candidates(Collection<Scenario> scenarios, Profile profile) {
        return scenarios.stream().filter(scenario -> scenario.eligibility().allows(profile)).toList();
    }

    static Scenario pick(List<Scenario> candidates, RandomGenerator random) {
        if (candidates.isEmpty()) {
            throw new IllegalStateException("이 프로필에 맞는 시나리오가 없어요.");
        }
        Map<ScamType, List<Scenario>> byType = new LinkedHashMap<>();
        candidates.forEach(scenario -> byType.computeIfAbsent(scenario.subType(), type -> new ArrayList<>()).add(scenario));
        List<ScamType> types = new ArrayList<>(byType.keySet());
        List<Scenario> pool = byType.get(types.get(random.nextInt(types.size())));
        return pool.get(random.nextInt(pool.size()));
    }
}
