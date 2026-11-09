package com.cnsHackathon.cns.report;

import com.cnsHackathon.cns.scenario.ScamType;
import com.cnsHackathon.cns.session.profile.AgeGroup;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 프로필 보정 점수표 (resources/scoring/difficulty.json).
 * 사용자 연령대에서 피해가 많은 유형을 막아 냈으면 가점을 준다. 특정 연령대를 깎는 표가 아니다.
 */
@Component
public class DifficultyTable {

    private static final String LOCATION = "scoring/difficulty.json";

    private final Map<ScamType, Map<AgeGroup, Integer>> bonusTable;

    public DifficultyTable(ObjectMapper objectMapper) {
        this.bonusTable = load(objectMapper);
    }

    public int bonus(ScamType type, AgeGroup ageGroup) {
        return bonusTable.getOrDefault(type, Map.of()).getOrDefault(ageGroup, 0);
    }

    private static Map<ScamType, Map<AgeGroup, Integer>> load(ObjectMapper objectMapper) {
        RawTable raw;
        try (InputStream in = new ClassPathResource(LOCATION).getInputStream()) {
            raw = objectMapper.readValue(in, RawTable.class);
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException(LOCATION + "를 읽지 못했어요: " + e.getMessage(), e);
        }

        Map<ScamType, Map<AgeGroup, Integer>> table = new EnumMap<>(ScamType.class);
        if (raw.bonus() == null) {
            return table;
        }
        raw.bonus().forEach((typeName, row) -> {
            Map<AgeGroup, Integer> byAge = new EnumMap<>(AgeGroup.class);
            if (row != null) {
                row.forEach((ageLabel, value) -> {
                    if (value == null || value < 0 || value > ScoreCalculator.MAX_BONUS) {
                        throw new IllegalStateException(LOCATION + "의 " + typeName + "/" + ageLabel
                                + " 값은 0~" + ScoreCalculator.MAX_BONUS + " 사이여야 해요.");
                    }
                    byAge.put(AgeGroup.from(ageLabel), value);
                });
            }
            table.put(ScamType.valueOf(typeName), byAge);
        });
        return table;
    }

    /** { "note": "...", "bonus": { "INSTITUTION": { "20대": 8 } } } */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawTable(String note, Map<String, Map<String, Integer>> bonus) {
    }
}
