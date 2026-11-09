package com.cnsHackathon.cns.report;

import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.conversation.domain.UtteranceRepository;
import com.cnsHackathon.cns.global.error.BusinessException;
import com.cnsHackathon.cns.global.error.ErrorCode;
import com.cnsHackathon.cns.llm.GeminiClient;
import com.cnsHackathon.cns.llm.LlmException;
import com.cnsHackathon.cns.report.domain.SessionReport;
import com.cnsHackathon.cns.report.domain.SessionReportRepository;
import com.cnsHackathon.cns.report.dto.ReportResponse;
import com.cnsHackathon.cns.scenario.Scenario;
import com.cnsHackathon.cns.scenario.ScenarioCatalog;
import com.cnsHackathon.cns.session.domain.CallSession;
import com.cnsHackathon.cns.session.domain.CallSessionRepository;
import com.cnsHackathon.cns.session.domain.EndReason;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * 통화가 끝난 뒤 첫 요청 때 리포트를 만들어 저장하고, 이후에는 저장본을 돌려준다.
 * 같은 통화의 리포트를 동시에 요청하면 AI는 한 번만 부르고 나머지 요청은 그 결과를 기다린다.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final CallSessionRepository sessions;
    private final UtteranceRepository utterances;
    private final SessionReportRepository reports;
    private final ScenarioCatalog scenarioCatalog;
    private final EvaluationPromptBuilder promptBuilder;
    private final GeminiClient gemini;
    private final ReportAssembler assembler;
    private final DifficultyTable difficultyTable;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final ConcurrentMap<String, CompletableFuture<ReportResponse>> generating = new ConcurrentHashMap<>();

    public ReportService(
            CallSessionRepository sessions,
            UtteranceRepository utterances,
            SessionReportRepository reports,
            ScenarioCatalog scenarioCatalog,
            EvaluationPromptBuilder promptBuilder,
            GeminiClient gemini,
            ReportAssembler assembler,
            DifficultyTable difficultyTable,
            ObjectMapper objectMapper,
            TransactionTemplate transaction,
            Clock clock) {
        this.sessions = sessions;
        this.utterances = utterances;
        this.reports = reports;
        this.scenarioCatalog = scenarioCatalog;
        this.promptBuilder = promptBuilder;
        this.gemini = gemini;
        this.assembler = assembler;
        this.difficultyTable = difficultyTable;
        this.objectMapper = objectMapper;
        this.transaction = transaction;
        this.clock = clock;
    }

    public ReportResponse getReport(String sessionId) {
        Optional<ReportResponse> saved = findSaved(sessionId);
        if (saved.isPresent()) {
            return saved.get();
        }

        CompletableFuture<ReportResponse> mine = new CompletableFuture<>();
        CompletableFuture<ReportResponse> running = generating.putIfAbsent(sessionId, mine);
        if (running != null) {
            return await(running);
        }
        try {
            ReportResponse report = findSaved(sessionId).orElseGet(() -> generate(sessionId));
            mine.complete(report);
            return report;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            generating.remove(sessionId, mine);
        }
    }

    private ReportResponse generate(String sessionId) {
        CallSession session = endedSession(sessionId);
        Scenario scenario = scenarioCatalog.get(session.getScenarioId());
        List<Utterance> transcript = utterances.findBySessionIdOrderBySeqAsc(sessionId);

        Evaluation evaluation = transcript.stream().anyMatch(Utterance::isUser)
                ? evaluate(scenario, session, transcript)
                : Evaluation.withoutAnswer();
        int bonus = difficultyTable.bonus(scenario.subType(), session.getAgeGroup());
        ReportResponse report = assembler.assemble(
                sessionId, session.getEndReason(), scenario, transcript, evaluation, bonus);

        String payload = objectMapper.writeValueAsString(report);
        transaction.executeWithoutResult(status -> reports.save(
                new SessionReport(sessionId, report.result(), report.score(), payload, clock.instant())));
        log.info("리포트 생성: session={}, result={}, score={}", sessionId, report.result(), report.score());
        return report;
    }

    /** 탭을 닫아 종료 요청이 오지 않은 통화는 마감이 지났으면 시간 초과로 닫는다. */
    private CallSession endedSession(String sessionId) {
        CallSession session = sessions.getOrThrow(sessionId);
        if (session.isEnded()) {
            return session;
        }
        Instant now = clock.instant();
        if (!session.isOverdue(now)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ENDED);
        }
        return transaction.execute(status -> {
            CallSession current = sessions.getOrThrow(sessionId);
            current.end(EndReason.TIMEOUT, now);
            return current;
        });
    }

    private Evaluation evaluate(Scenario scenario, CallSession session, List<Utterance> transcript) {
        try {
            return gemini.generateJson(
                    promptBuilder.build(scenario, session.profile(), session.getEndReason(), transcript),
                    Evaluation.class,
                    Evaluation::isUsable);
        } catch (LlmException e) {
            throw new BusinessException(ErrorCode.LLM_UNAVAILABLE, "리포트를 만들지 못했어요. 다시 시도해 주세요.");
        }
    }

    private Optional<ReportResponse> findSaved(String sessionId) {
        return reports.findById(sessionId)
                .map(saved -> objectMapper.readValue(saved.getPayload(), ReportResponse.class));
    }

    private static ReportResponse await(CompletableFuture<ReportResponse> running) {
        try {
            return running.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }
}
