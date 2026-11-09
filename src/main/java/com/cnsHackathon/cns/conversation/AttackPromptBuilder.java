package com.cnsHackathon.cns.conversation;

import com.cnsHackathon.cns.conversation.domain.CallPhase;
import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.global.support.Texts;
import com.cnsHackathon.cns.llm.LlmMessage;
import com.cnsHackathon.cns.llm.LlmPrompt;
import com.cnsHackathon.cns.llm.LlmTask;
import com.cnsHackathon.cns.llm.PromptTemplate;
import com.cnsHackathon.cns.scenario.Scenario;
import com.cnsHackathon.cns.session.profile.Profile;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 사기범 역할 프롬프트를 만든다. 규칙 문구는 resources/prompts/attack-system.txt에 있다.
 *
 * 대화 구성: [통화 연결 안내(user)] → 사기범(model) → 사용자(user) → … → [진행 정보 + 이번 답변(user)]
 */
@Component
public class AttackPromptBuilder {

    static final String CALL_CONNECTED = "[통화 연결] 상대방이 전화를 받았다. 등장인물 C가 첫 발화로 통화를 시작해라.";

    private final PromptTemplate systemTemplate = PromptTemplate.load("prompts/attack-system.txt");
    private final ObjectMapper objectMapper;

    public AttackPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LlmPrompt opening(Scenario scenario, Profile profile, Duration timeLimit) {
        String cue = progressNote(CallContext.opening(timeLimit)) + "\n" + CALL_CONNECTED;
        return prompt(scenario, profile, List.of(LlmMessage.user(cue)));
    }

    public LlmPrompt reply(Scenario scenario, Profile profile, List<Utterance> history, String userText,
                           CallContext context) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.user(CALL_CONNECTED));
        for (Utterance utterance : history) {
            messages.add(utterance.isUser()
                    ? LlmMessage.user(userLine(utterance.getText()))
                    : LlmMessage.model(scammerLine(utterance)));
        }
        messages.add(LlmMessage.user(progressNote(context) + "\n" + userLine(userText)));
        return prompt(scenario, profile, messages);
    }

    private LlmPrompt prompt(Scenario scenario, Profile profile, List<LlmMessage> messages) {
        String system = systemTemplate.render(Map.of(
                "scenario", describe(scenario),
                "profile", profile.describe(),
                "phasePlan", CallPhase.plan()));
        return new LlmPrompt(LlmTask.TURN, system, messages, ScammerReply.schema(scenario.speakerKeys()));
    }

    static String progressNote(CallContext context) {
        CallPhase phase = context.recommendedPhase();
        String note = "[진행 정보] 경과 " + Texts.minutesAndSeconds(context.elapsed())
                + " / 남은 시간 " + Texts.minutesAndSeconds(context.remaining())
                + " / 권장 단계: " + phase.label() + "(" + phase.name() + ")";
        if (context.lastTurn()) {
            note += "\n[마지막 발화] 통화 시간이 끝났다. 이번 발화로 통화를 마무리하고 ended=true로 답해라.";
        }
        return note;
    }

    static String describe(Scenario scenario) {
        StringBuilder text = new StringBuilder();
        text.append("제목: ").append(scenario.title()).append('\n');
        text.append("유형: ").append(scenario.subType().label()).append('\n');
        text.append("등장인물:\n");
        scenario.personas().forEach(persona ->
                text.append("- ").append(persona.key()).append(": ").append(persona.role()).append('\n'));
        text.append("진행 흐름:\n");
        List<String> flow = scenario.flow();
        for (int i = 0; i < flow.size(); i++) {
            text.append(i + 1).append(". ").append(flow.get(i)).append('\n');
        }
        text.append("최종 요구: ").append(scenario.demand());
        return text.toString();
    }

    private static String userLine(String text) {
        return "사용자: " + text;
    }

    /** 이전 사기범 발화는 AI가 답한 형식(JSON) 그대로 넣어 흐름을 잇게 한다. */
    private String scammerLine(Utterance utterance) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("speaker", utterance.getSpeaker());
        line.put("utterance", utterance.getText());
        line.put("phase", utterance.getPhase() == null ? null : utterance.getPhase().name());
        return objectMapper.writeValueAsString(line);
    }
}
