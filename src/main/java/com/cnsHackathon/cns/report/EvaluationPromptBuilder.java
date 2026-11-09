package com.cnsHackathon.cns.report;

import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.llm.LlmMessage;
import com.cnsHackathon.cns.llm.LlmPrompt;
import com.cnsHackathon.cns.llm.LlmTask;
import com.cnsHackathon.cns.llm.PromptTemplate;
import com.cnsHackathon.cns.scenario.ScamType;
import com.cnsHackathon.cns.scenario.Scenario;
import com.cnsHackathon.cns.session.domain.EndReason;
import com.cnsHackathon.cns.session.profile.Profile;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 평가 프롬프트. 규칙 문구는 resources/prompts/evaluation-system.txt에 있다. */
@Component
public class EvaluationPromptBuilder {

    private final PromptTemplate systemTemplate = PromptTemplate.load("prompts/evaluation-system.txt");

    public LlmPrompt build(Scenario scenario, Profile profile, EndReason endReason, List<Utterance> transcript) {
        String request = String.join("\n\n",
                section("시나리오", "제목: " + scenario.title()
                        + "\n유형: " + scenario.subType().label()
                        + "\n사기범의 최종 요구: " + scenario.demand()
                        + "\n위험 신호:\n" + bullets(scenario.redFlags())),
                section("체크포인트", scenario.checkpoints().stream()
                        .map(checkpoint -> "- " + checkpoint.id() + ": " + checkpoint.desc())
                        .collect(Collectors.joining("\n"))),
                section("참고 대응 수칙", bullets(scenario.tips())),
                section("사용자 정보", profile.describe()),
                section("통화 종료 사유", endReason == null ? "알 수 없음" : endReason.description()),
                section("통화 기록", transcriptText(transcript)),
                section("관련 사기 유형 선택지", Arrays.stream(ScamType.values())
                        .map(type -> "- " + type.name() + ": " + type.label())
                        .collect(Collectors.joining("\n"))));

        List<String> checkpointIds = scenario.checkpoints().stream().map(Scenario.Checkpoint::id).toList();
        return new LlmPrompt(
                LlmTask.REPORT,
                systemTemplate.render(Map.of()),
                List.of(LlmMessage.user(request)),
                Evaluation.schema(checkpointIds));
    }

    static String transcriptText(List<Utterance> transcript) {
        return transcript.stream()
                .map(line -> "[턴 " + line.getTurnNo() + "] "
                        + (line.isUser() ? "사용자" : "상대방(" + line.getSpeaker() + ")")
                        + ": " + line.getText())
                .collect(Collectors.joining("\n"));
    }

    private static String section(String title, String body) {
        return "## " + title + "\n" + body;
    }

    private static String bullets(List<String> items) {
        if (items.isEmpty()) {
            return "- (없음)";
        }
        return items.stream().map(item -> "- " + item).collect(Collectors.joining("\n"));
    }
}
