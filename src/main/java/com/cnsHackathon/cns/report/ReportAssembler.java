package com.cnsHackathon.cns.report;

import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.global.support.Texts;
import com.cnsHackathon.cns.report.dto.ReportResponse;
import com.cnsHackathon.cns.report.dto.ReportResponse.CheckpointResult;
import com.cnsHackathon.cns.report.dto.ReportResponse.Moment;
import com.cnsHackathon.cns.report.dto.ReportResponse.ScenarioInfo;
import com.cnsHackathon.cns.scenario.Scenario;
import com.cnsHackathon.cns.session.domain.EndReason;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 평가 AI의 판정 근거와 실제 대화 기록을 합쳐 리포트를 만든다.
 * AI가 없는 턴 번호를 대거나 인용문을 지어내도 리포트가 틀리지 않도록 대화 기록을 기준으로 맞춘다.
 */
@Component
public class ReportAssembler {

    private static final int MAX_MOMENTS = 3;
    private static final int MAX_TIPS = 3;
    private static final int MAX_RELATED_TYPES = 3;

    public ReportResponse assemble(String sessionId, EndReason endReason, Scenario scenario,
                                   List<Utterance> transcript, Evaluation evaluation, int profileBonus) {
        Map<Integer, String> answers = userAnswersByTurn(transcript);
        boolean scammed = endReason == EndReason.SCAMMED || evaluation.result() == ReportResult.SCAMMED;
        ReportResult result = scammed ? ReportResult.SCAMMED : ReportResult.SAFE;
        Integer scammedTurn = scammed ? scammedTurn(evaluation, answers) : null;

        List<CheckpointResult> checkpoints = checkpointResults(scenario, evaluation, answers);
        List<Moment> riskyMoments = moments(evaluation.riskyMoments(), answers);
        if (scammedTurn != null) {
            riskyMoments = withScammedMomentFirst(riskyMoments, scammedTurn, answers, evaluation);
        }

        return new ReportResponse(
                sessionId,
                ScoreCalculator.score(result, checkpoints, scammedTurn, profileBonus),
                result,
                Texts.hasText(evaluation.summary()) ? evaluation.summary().trim() : "통화 대응을 정리했어요.",
                scammedTurn,
                scammed ? scammedAction(evaluation, scenario) : null,
                limit(riskyMoments, MAX_MOMENTS),
                limit(moments(evaluation.goodMoments(), answers), MAX_MOMENTS),
                tips(evaluation, scenario),
                limit(evaluation.relatedScamTypes().stream().distinct().toList(), MAX_RELATED_TYPES),
                checkpoints,
                new ScenarioInfo(scenario.id(), scenario.subType(), scenario.subType().label(), scenario.title()));
    }

    /** 턴 번호 → 그 턴에 사용자가 한 말 */
    private static Map<Integer, String> userAnswersByTurn(List<Utterance> transcript) {
        Map<Integer, String> answers = new LinkedHashMap<>();
        for (Utterance utterance : transcript) {
            if (utterance.isUser()) {
                answers.merge(utterance.getTurnNo(), utterance.getText(), (first, next) -> first + " " + next);
            }
        }
        return answers;
    }

    /** AI가 고른 턴이 실제 답변이 아니면 마지막 답변을 당한 지점으로 본다. */
    private static Integer scammedTurn(Evaluation evaluation, Map<Integer, String> answers) {
        if (answers.containsKey(evaluation.scammedTurn())) {
            return evaluation.scammedTurn();
        }
        return answers.keySet().stream().reduce((first, second) -> second).orElse(null);
    }

    private static List<CheckpointResult> checkpointResults(Scenario scenario, Evaluation evaluation,
                                                            Map<Integer, String> answers) {
        Map<String, Evaluation.CheckpointCheck> checks = new LinkedHashMap<>();
        evaluation.checkpoints().forEach(check -> checks.putIfAbsent(check.id(), check));
        return scenario.checkpoints().stream()
                .map(checkpoint -> {
                    Evaluation.CheckpointCheck check = checks.get(checkpoint.id());
                    boolean achieved = check != null && check.achieved();
                    Integer turn = achieved && answers.containsKey(check.turn()) ? check.turn() : null;
                    return new CheckpointResult(checkpoint.id(), checkpoint.desc(), checkpoint.weight(), achieved, turn);
                })
                .toList();
    }

    /** 실제 답변이 있는 턴만 남기고, 인용문은 대화 기록에서 꺼낸다. 같은 턴은 한 번만. */
    private static List<Moment> moments(List<Evaluation.Note> notes, Map<Integer, String> answers) {
        List<Moment> moments = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Evaluation.Note note : notes) {
            if (answers.containsKey(note.turn()) && seen.add(note.turn())) {
                String why = Texts.hasText(note.why()) ? note.why().trim() : "";
                moments.add(new Moment(note.turn(), answers.get(note.turn()), why));
            }
        }
        return moments;
    }

    private static List<Moment> withScammedMomentFirst(List<Moment> riskyMoments, int scammedTurn,
                                                       Map<Integer, String> answers, Evaluation evaluation) {
        List<Moment> ordered = new ArrayList<>();
        Moment scammedMoment = riskyMoments.stream()
                .filter(moment -> moment.turn() == scammedTurn)
                .findFirst()
                .orElseGet(() -> new Moment(scammedTurn, answers.get(scammedTurn),
                        Texts.hasText(evaluation.scammedAction())
                                ? evaluation.scammedAction().trim()
                                : "이 답변에서 사기범의 요구에 응했어요."));
        ordered.add(scammedMoment);
        riskyMoments.stream().filter(moment -> moment.turn() != scammedTurn).forEach(ordered::add);
        return ordered;
    }

    private static String scammedAction(Evaluation evaluation, Scenario scenario) {
        if (Texts.hasText(evaluation.scammedAction())) {
            return evaluation.scammedAction().trim();
        }
        return "사기범의 요구(" + scenario.demand() + ")에 응했어요.";
    }

    private static List<String> tips(Evaluation evaluation, Scenario scenario) {
        List<String> tips = evaluation.realWorldTips().stream()
                .filter(Texts::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return limit(tips.isEmpty() ? scenario.tips() : tips, MAX_TIPS);
    }

    private static <T> List<T> limit(List<T> items, int max) {
        return items.size() <= max ? List.copyOf(items) : List.copyOf(items.subList(0, max));
    }
}
