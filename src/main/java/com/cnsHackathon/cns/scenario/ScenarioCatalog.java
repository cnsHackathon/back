package com.cnsHackathon.cns.scenario;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * resources/scenarios/*.json을 서버 시작 때 한 번 읽는다.
 * 시나리오를 추가하려면 JSON 파일만 넣고 서버를 다시 켜면 된다.
 */
@Component
public class ScenarioCatalog {

    private static final Logger log = LoggerFactory.getLogger(ScenarioCatalog.class);
    private static final String LOCATION = "classpath*:scenarios/*.json";

    private final Map<String, Scenario> scenariosById;

    public ScenarioCatalog(ObjectMapper objectMapper) {
        this.scenariosById = Collections.unmodifiableMap(load(objectMapper));
        log.info("시나리오 {}개를 읽었어요: {}", scenariosById.size(), scenariosById.keySet());
    }

    public List<Scenario> all() {
        return List.copyOf(scenariosById.values());
    }

    public Optional<Scenario> find(String id) {
        return Optional.ofNullable(scenariosById.get(id));
    }

    public Scenario get(String id) {
        return find(id).orElseThrow(() -> new IllegalStateException("시나리오가 없어요: " + id));
    }

    private static Map<String, Scenario> load(ObjectMapper objectMapper) {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
        } catch (IOException e) {
            throw new IllegalStateException("시나리오 폴더를 읽지 못했어요.", e);
        }

        Map<String, Scenario> scenarios = new LinkedHashMap<>();
        for (Resource resource : resources) {
            Scenario scenario = read(objectMapper, resource);
            List<String> problems = ScenarioValidator.problems(scenario);
            if (!problems.isEmpty()) {
                throw new IllegalStateException(resource.getFilename() + " 시나리오를 고쳐 주세요: " + problems);
            }
            if (scenarios.put(scenario.id(), scenario) != null) {
                throw new IllegalStateException("시나리오 id가 겹쳐요: " + scenario.id());
            }
        }

        if (scenarios.isEmpty()) {
            throw new IllegalStateException("resources/scenarios에 시나리오가 하나도 없어요.");
        }
        boolean someoneCanAlwaysGetOne = scenarios.values().stream()
                .anyMatch(scenario -> scenario.eligibility().isOpenToAll());
        if (!someoneCanAlwaysGetOne) {
            throw new IllegalStateException("조건 없이 누구에게나 나갈 수 있는 시나리오가 최소 1개 필요해요.");
        }
        return scenarios;
    }

    private static Scenario read(ObjectMapper objectMapper, Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, Scenario.class);
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException(resource.getFilename() + " 시나리오 JSON을 읽지 못했어요: " + e.getMessage(), e);
        }
    }
}
