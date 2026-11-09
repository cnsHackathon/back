package com.cnsHackathon.cns.report.dto;

import com.cnsHackathon.cns.report.ReportResult;
import com.cnsHackathon.cns.scenario.ScamType;
import java.util.List;

/**
 * 리포트 화면용 응답.
 * - 당함: scammedTurn과 같은 턴의 항목이 riskyMoments에 반드시 들어 있다. (프론트가 인용문으로 보여 준다)
 * - 안 당함: scammedTurn, scammedAction은 null
 * - quote는 사용자가 실제로 한 말을 서버가 대화 기록에서 꺼내 넣는다.
 * - checkpoints, scenario는 프론트가 쓰지 않지만 확인용으로 보낸다.
 */
public record ReportResponse(
        String sessionId,
        int score,
        ReportResult result,
        String summary,
        Integer scammedTurn,
        String scammedAction,
        List<Moment> riskyMoments,
        List<Moment> goodMoments,
        List<String> realWorldTips,
        List<ScamType> relatedScamTypes,
        List<CheckpointResult> checkpoints,
        ScenarioInfo scenario) {

    public record Moment(int turn, String quote, String why) {
    }

    public record CheckpointResult(String id, String description, int weight, boolean achieved, Integer turn) {
    }

    public record ScenarioInfo(String id, ScamType type, String typeLabel, String title) {
    }
}
