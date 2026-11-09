package com.cnsHackathon.cns.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnsHackathon.cns.conversation.domain.CallPhase;
import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.report.dto.ReportResponse;
import com.cnsHackathon.cns.scenario.ScamType;
import com.cnsHackathon.cns.scenario.Scenario;
import com.cnsHackathon.cns.session.domain.EndReason;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportAssemblerTest {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");

    private final ReportAssembler assembler = new ReportAssembler();

    private final Scenario scenario = new Scenario(
            "noshow-test", ScamType.NOSHOW_PROXY_PURCHASE, "노쇼 테스트", null,
            List.of(new Scenario.Persona("C", "군 간부"), new Scenario.Persona("B", "업체")),
            List.of("주문"), "대금 대납", List.of(),
            List.of(new Scenario.Checkpoint("EXPRESS_DOUBT", "의심", 25),
                    new Scenario.Checkpoint("VERIFY_OFFICIAL", "대표번호 확인", 35),
                    new Scenario.Checkpoint("REFUSE_PROXY_PAY", "대납 거절", 40)),
            List.of("군은 대납을 요구하지 않아요."), List.of());

    private final List<Utterance> transcript = List.of(
            Utterance.scammer("s", 0, 1, "C", "사장님, 단체 주문입니다.", CallPhase.APPROACH, "통화 시작", NOW),
            Utterance.user("s", 1, 1, "네 몇 개요?", NOW),
            Utterance.scammer("s", 2, 2, "C", "업체 연결해 드릴게요.", CallPhase.DEMAND, "", NOW),
            Utterance.user("s", 3, 2, "네 송금할게요", NOW),
            Utterance.scammer("s", 4, 3, "B", "감사합니다.", CallPhase.CLOSING, "", NOW));

    @Test
    void 당한_경우_당한_턴의_실제_발화가_위험한_순간_맨_앞에_들어간다() {
        Evaluation evaluation = new Evaluation(ReportResult.SCAMMED, "요약", 2, "대납에 동의했어요.",
                List.of(), List.of(new Evaluation.Note(1, "확인 없이 주문을 받았어요.")), List.of(),
                List.of(), List.of(ScamType.LOAN, ScamType.LOAN));

        ReportResponse report = assembler.assemble("s", EndReason.SCAMMED, scenario, transcript, evaluation, 0);

        assertEquals(ReportResult.SCAMMED, report.result());
        assertEquals(2, report.scammedTurn());
        assertEquals(2, report.riskyMoments().get(0).turn());
        assertEquals("네 송금할게요", report.riskyMoments().get(0).quote());
        assertEquals(List.of(ScamType.LOAN), report.relatedScamTypes());
        // 평가가 팁을 주지 않으면 시나리오의 팁을 쓴다.
        assertEquals(List.of("군은 대납을 요구하지 않아요."), report.realWorldTips());
    }

    @Test
    void 평가가_없는_턴을_고르면_마지막_답변을_당한_지점으로_본다() {
        Evaluation evaluation = new Evaluation(ReportResult.SCAMMED, "요약", 9, "",
                List.of(), List.of(), List.of(), List.of(), List.of());

        ReportResponse report = assembler.assemble("s", EndReason.SCAMMED, scenario, transcript, evaluation, 0);

        assertEquals(2, report.scammedTurn());
        assertEquals("네 송금할게요", report.riskyMoments().get(0).quote());
        assertTrue(report.scammedAction().contains("대금 대납"));
    }

    @Test
    void 안_당한_경우_당한_턴은_null이고_없는_턴의_순간은_버린다() {
        Evaluation evaluation = new Evaluation(ReportResult.SAFE, "잘 대처했어요.", 0, "",
                List.of(new Evaluation.CheckpointCheck("EXPRESS_DOUBT", true, 1),
                        new Evaluation.CheckpointCheck("VERIFY_OFFICIAL", true, 7)),
                List.of(), List.of(new Evaluation.Note(1, "의심했어요."), new Evaluation.Note(5, "없는 턴")),
                List.of("대표번호로 확인하세요."), List.of());

        ReportResponse report = assembler.assemble("s", EndReason.DETECTED, scenario, transcript, evaluation, 0);

        assertEquals(ReportResult.SAFE, report.result());
        assertNull(report.scammedTurn());
        assertNull(report.scammedAction());
        assertEquals(1, report.goodMoments().size());
        assertEquals("네 몇 개요?", report.goodMoments().get(0).quote());
        // 없는 턴(7)에 달성했다고 해도 달성은 인정하되 턴은 비운다.
        ReportResponse.CheckpointResult verify = report.checkpoints().get(1);
        assertTrue(verify.achieved());
        assertNull(verify.turn());
        assertFalse(report.checkpoints().get(2).achieved());
        // 50 + 0.4 × (25 + 35)
        assertEquals(74, report.score());
    }

    @Test
    void 대화가_SCAMMED로_끝났으면_평가가_SAFE라도_당함으로_본다() {
        Evaluation evaluation = new Evaluation(ReportResult.SAFE, "요약", 0, "",
                List.of(), List.of(), List.of(), List.of(), List.of());

        ReportResponse report = assembler.assemble("s", EndReason.SCAMMED, scenario, transcript, evaluation, 0);

        assertEquals(ReportResult.SCAMMED, report.result());
        assertEquals(2, report.scammedTurn());
    }
}
