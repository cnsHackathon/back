package com.cnsHackathon.cns.conversation;

import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.conversation.domain.UtteranceRepository;
import com.cnsHackathon.cns.conversation.dto.TurnRequest;
import com.cnsHackathon.cns.conversation.dto.TurnResponse;
import com.cnsHackathon.cns.global.config.AppProperties;
import com.cnsHackathon.cns.global.error.BusinessException;
import com.cnsHackathon.cns.global.error.ErrorCode;
import com.cnsHackathon.cns.llm.GeminiClient;
import com.cnsHackathon.cns.llm.LlmException;
import com.cnsHackathon.cns.llm.LlmPrompt;
import com.cnsHackathon.cns.scenario.Scenario;
import com.cnsHackathon.cns.scenario.ScenarioCatalog;
import com.cnsHackathon.cns.session.domain.CallSession;
import com.cnsHackathon.cns.session.domain.CallSessionRepository;
import com.cnsHackathon.cns.session.domain.EndReason;
import com.cnsHackathon.cns.session.profile.Profile;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 사용자 답변을 받아 사기범의 다음 발화를 만든다.
 *
 * AI 호출은 몇 초 걸리므로 트랜잭션 밖에서 하고, 성공했을 때만 사용자 답변과 사기범 발화를 함께 저장한다.
 * 그래서 실패 후 프론트가 같은 답변으로 재시도해도 기록이 중복되지 않는다.
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final CallSessionRepository sessions;
    private final UtteranceRepository utterances;
    private final ScenarioCatalog scenarioCatalog;
    private final AttackPromptBuilder promptBuilder;
    private final GeminiClient gemini;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final Duration timeLimit;
    private final Set<String> busySessions = ConcurrentHashMap.newKeySet();

    public ConversationService(
            CallSessionRepository sessions,
            UtteranceRepository utterances,
            ScenarioCatalog scenarioCatalog,
            AttackPromptBuilder promptBuilder,
            GeminiClient gemini,
            TransactionTemplate transaction,
            Clock clock,
            AppProperties appProperties) {
        this.sessions = sessions;
        this.utterances = utterances;
        this.scenarioCatalog = scenarioCatalog;
        this.promptBuilder = promptBuilder;
        this.gemini = gemini;
        this.transaction = transaction;
        this.clock = clock;
        this.timeLimit = appProperties.callTimeLimit();
    }

    /** 세션을 만들 때 사기범 첫 발화를 준비한다. 저장은 호출한 쪽에서 한다. */
    public ScammerReply composeOpening(Scenario scenario, Profile profile) {
        return ask(promptBuilder.opening(scenario, profile, timeLimit), scenario);
    }

    public TurnResponse reply(String sessionId, TurnRequest request) {
        if (!busySessions.add(sessionId)) {
            throw new BusinessException(ErrorCode.TURN_IN_PROGRESS);
        }
        try {
            return replyAlone(sessionId, request.userText().trim(), request.finalRequested());
        } finally {
            busySessions.remove(sessionId);
        }
    }

    private TurnResponse replyAlone(String sessionId, String userText, boolean finalRequested) {
        Instant receivedAt = clock.instant();
        CallSession session = sessions.getOrThrow(sessionId);
        if (session.isEnded()) {
            throw new BusinessException(ErrorCode.SESSION_ENDED);
        }
        if (!session.isStarted()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_STARTED);
        }

        // 마감 뒤에 도착한 답변도 마지막 답변으로 받아 사기범이 마무리 인사를 하게 한다.
        boolean lastTurn = finalRequested || session.isOverdue(receivedAt);
        Scenario scenario = scenarioCatalog.get(session.getScenarioId());
        List<Utterance> history = utterances.findBySessionIdOrderBySeqAsc(sessionId);
        CallContext context = new CallContext(
                session.elapsed(receivedAt), session.remaining(receivedAt, timeLimit), lastTurn);

        ScammerReply reply = ask(
                promptBuilder.reply(scenario, session.profile(), history, userText, context), scenario);
        EndReason endReason = reply.toEndReason(lastTurn);

        int answeredTurn = history.isEmpty() ? 1 : history.get(history.size() - 1).getTurnNo();
        Instant repliedAt = clock.instant();
        Optional<TurnResponse> response = transaction.execute(status -> {
            CallSession current = sessions.getOrThrow(sessionId);
            utterances.save(Utterance.user(sessionId, history.size(), answeredTurn, userText, receivedAt));
            if (current.isEnded()) {
                // AI를 기다리는 사이 사용자가 끊었다. 답변은 평가에 쓰도록 남기고 사기범 발화는 버린다.
                return Optional.<TurnResponse>empty();
            }
            Utterance line = utterances.save(reply.toUtterance(sessionId, history.size() + 1, answeredTurn + 1, repliedAt));
            if (endReason != null) {
                current.end(endReason, repliedAt);
            }
            return Optional.of(TurnResponse.of(line, endReason, current.remaining(repliedAt, timeLimit)));
        });

        if (endReason != null) {
            log.info("통화 종료: session={}, reason={}", sessionId, endReason);
        }
        return response.orElseThrow(() -> new BusinessException(ErrorCode.SESSION_ENDED));
    }

    private ScammerReply ask(LlmPrompt prompt, Scenario scenario) {
        Set<String> speakers = scenario.speakerKeys();
        try {
            return gemini.generateJson(prompt, ScammerReply.class, reply -> reply.isUsable(speakers));
        } catch (LlmException e) {
            throw new BusinessException(ErrorCode.LLM_UNAVAILABLE);
        }
    }
}
