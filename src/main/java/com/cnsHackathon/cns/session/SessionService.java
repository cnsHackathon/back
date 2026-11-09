package com.cnsHackathon.cns.session;

import com.cnsHackathon.cns.conversation.ConversationService;
import com.cnsHackathon.cns.conversation.ScammerReply;
import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.conversation.domain.UtteranceRepository;
import com.cnsHackathon.cns.conversation.dto.TurnResponse;
import com.cnsHackathon.cns.global.config.AppProperties;
import com.cnsHackathon.cns.global.error.BusinessException;
import com.cnsHackathon.cns.global.error.ErrorCode;
import com.cnsHackathon.cns.global.support.Texts;
import com.cnsHackathon.cns.scenario.Scenario;
import com.cnsHackathon.cns.scenario.ScenarioCatalog;
import com.cnsHackathon.cns.scenario.ScenarioSelector;
import com.cnsHackathon.cns.session.domain.CallSession;
import com.cnsHackathon.cns.session.domain.CallSessionRepository;
import com.cnsHackathon.cns.session.dto.CreateSessionRequest;
import com.cnsHackathon.cns.session.dto.CreateSessionResponse;
import com.cnsHackathon.cns.session.dto.EndSessionRequest;
import com.cnsHackathon.cns.session.dto.EndSessionResponse;
import com.cnsHackathon.cns.session.dto.StartSessionResponse;
import com.cnsHackathon.cns.session.profile.Profile;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final CallSessionRepository sessions;
    private final UtteranceRepository utterances;
    private final ScenarioCatalog scenarioCatalog;
    private final ScenarioSelector scenarioSelector;
    private final ConversationService conversationService;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final Duration timeLimit;

    public SessionService(
            CallSessionRepository sessions,
            UtteranceRepository utterances,
            ScenarioCatalog scenarioCatalog,
            ScenarioSelector scenarioSelector,
            ConversationService conversationService,
            TransactionTemplate transaction,
            Clock clock,
            AppProperties appProperties) {
        this.sessions = sessions;
        this.utterances = utterances;
        this.scenarioCatalog = scenarioCatalog;
        this.scenarioSelector = scenarioSelector;
        this.conversationService = conversationService;
        this.transaction = transaction;
        this.clock = clock;
        this.timeLimit = appProperties.callTimeLimit();
    }

    /**
     * 시나리오를 배정하고 사기범 첫 발화까지 만든다.
     * AI 호출이 실패하면 아무것도 저장하지 않으므로 프론트는 같은 요청을 다시 보내면 된다.
     */
    public CreateSessionResponse create(CreateSessionRequest request) {
        Profile profile = request.toProfile();
        Scenario scenario = chooseScenario(request.scenarioId(), profile);
        CallSession session = CallSession.create(profile, scenario.id(), clock.instant());

        ScammerReply opening = conversationService.composeOpening(scenario, profile);
        Utterance firstLine = opening.toUtterance(session.getId(), 0, 1, clock.instant());
        transaction.executeWithoutResult(status -> {
            sessions.save(session);
            utterances.save(firstLine);
        });

        log.info("통화 준비: session={}, scenario={}", session.getId(), scenario.id());
        int timeLimitSec = Texts.ceilSeconds(timeLimit);
        return new CreateSessionResponse(session.getId(), timeLimitSec, TurnResponse.of(firstLine, null, timeLimit));
    }

    /** 받기를 누를 때 부른다. 이때부터 10분을 잰다. */
    @Transactional
    public StartSessionResponse start(String sessionId) {
        CallSession session = sessions.getOrThrow(sessionId);
        if (session.isEnded()) {
            throw new BusinessException(ErrorCode.SESSION_ENDED);
        }
        Instant now = clock.instant();
        session.start(now, timeLimit);
        return StartSessionResponse.of(session, now, timeLimit);
    }

    /**
     * 사용자가 끊거나 시간이 다 됐을 때 부른다.
     * 답변을 기다리는 사이에 서버가 먼저 통화를 끝냈을 수 있으므로, 이미 끝난 통화여도 성공으로 응답한다.
     */
    @Transactional
    public EndSessionResponse end(String sessionId, EndSessionRequest request) {
        if (!request.reason().isClientReason()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 사유는 HANG_UP 또는 TIMEOUT만 보낼 수 있어요.");
        }
        CallSession session = sessions.getOrThrow(sessionId);
        session.end(request.reason(), clock.instant());
        return new EndSessionResponse(true, session.getEndReason());
    }

    private Scenario chooseScenario(String requestedId, Profile profile) {
        if (requestedId == null || requestedId.isBlank()) {
            return scenarioSelector.select(profile);
        }
        return scenarioCatalog.find(requestedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "없는 시나리오예요: " + requestedId));
    }
}
