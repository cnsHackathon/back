package com.cnsHackathon.cns.report;

import com.cnsHackathon.cns.llm.JsonSchemas;
import com.cnsHackathon.cns.scenario.ScamType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * 평가 AI가 돌려주는 판정 근거. 점수는 AI가 아니라 ScoreCalculator가 계산한다.
 * 값이 없을 때 턴 번호는 0, 문장은 빈 문자열로 온다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Evaluation(
        ReportResult result,
        String summary,
        Integer scammedTurn,
        String scammedAction,
        List<CheckpointCheck> checkpoints,
        List<Note> riskyMoments,
        List<Note> goodMoments,
        List<String> realWorldTips,
        List<ScamType> relatedScamTypes) {

    // Jackson 3는 빠지거나 null인 int/boolean을 오류로 처리하므로 감싼 타입으로 받고 기본값을 채운다.
    public Evaluation {
        summary = summary == null ? "" : summary;
        scammedTurn = scammedTurn == null ? 0 : scammedTurn;
        scammedAction = scammedAction == null ? "" : scammedAction;
        checkpoints = checkpoints == null ? List.of() : List.copyOf(checkpoints);
        riskyMoments = riskyMoments == null ? List.of() : List.copyOf(riskyMoments);
        goodMoments = goodMoments == null ? List.of() : List.copyOf(goodMoments);
        realWorldTips = realWorldTips == null ? List.of() : List.copyOf(realWorldTips);
        relatedScamTypes = relatedScamTypes == null ? List.of() : List.copyOf(relatedScamTypes);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CheckpointCheck(String id, Boolean achieved, Integer turn) {

        public CheckpointCheck {
            achieved = Boolean.TRUE.equals(achieved);
            turn = turn == null ? 0 : turn;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Note(Integer turn, String why) {

        public Note {
            turn = turn == null ? 0 : turn;
        }
    }

    /** 사용자가 한 마디도 하지 않고 끊었을 때. AI를 부르지 않는다. */
    public static Evaluation withoutAnswer() {
        return new Evaluation(
                ReportResult.SAFE,
                "답변하지 않고 통화를 마쳤어요. 낯선 번호의 수상한 전화는 대응하지 않고 끊는 것도 좋은 방법이에요.",
                0, "", List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public boolean isUsable() {
        return result != null && !summary.isBlank();
    }

    public static Map<String, Object> schema(List<String> checkpointIds) {
        Map<String, Object> note = JsonSchemas.object(JsonSchemas.props(
                "turn", JsonSchemas.integer("사용자 답변의 턴 번호"),
                "why", JsonSchemas.string("이유 한 문장. 사용자에게 말하듯 '~했어요' 체")));
        Map<String, Object> checkpoint = JsonSchemas.object(JsonSchemas.props(
                "id", JsonSchemas.enumOf(checkpointIds, "체크포인트 ID"),
                "achieved", JsonSchemas.bool("달성했으면 true"),
                "turn", JsonSchemas.integer("처음 달성한 턴 번호. 달성하지 못했으면 0")));
        return JsonSchemas.object(JsonSchemas.props(
                "result", JsonSchemas.enumOf(ReportResult.values(), "사기에 넘어갔으면 SCAMMED, 아니면 SAFE"),
                "summary", JsonSchemas.string("통화 전체 요약 2~3문장"),
                "scammedTurn", JsonSchemas.integer("처음 넘어간 사용자 답변의 턴 번호. SAFE면 0"),
                "scammedAction", JsonSchemas.string("무엇에 응했는지 한 문장. SAFE면 빈 문자열"),
                "checkpoints", JsonSchemas.arrayOf(checkpoint, "시나리오의 모든 체크포인트"),
                "riskyMoments", JsonSchemas.arrayOf(note, "위험했던 사용자 답변. 최대 3개"),
                "goodMoments", JsonSchemas.arrayOf(note, "잘 대처한 사용자 답변. 최대 3개"),
                "realWorldTips", JsonSchemas.arrayOf(JsonSchemas.string(null), "실제 통화에서 기억할 행동 수칙 2~3개"),
                "relatedScamTypes", JsonSchemas.arrayOf(JsonSchemas.enumOf(ScamType.values(), null),
                        "이번 대응을 보고 주의해야 할 사기 유형 1~3개")));
    }
}
