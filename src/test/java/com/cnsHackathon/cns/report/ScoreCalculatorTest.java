package com.cnsHackathon.cns.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cnsHackathon.cns.report.dto.ReportResponse.CheckpointResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private static final List<CheckpointResult> CHECKPOINTS = List.of(
            new CheckpointResult("EXPRESS_DOUBT", "의심", 25, true, 2),
            new CheckpointResult("VERIFY_OFFICIAL", "대표번호 확인", 35, true, 4),
            new CheckpointResult("REFUSE_PROXY_PAY", "대납 거절", 40, false, null));

    @Test
    void 안_당하면_50점부터_시작해서_달성한_가중치를_더한다() {
        // 50 + 0.4 × 60 + 0
        assertEquals(74, ScoreCalculator.score(ReportResult.SAFE, CHECKPOINTS, null, 0));
    }

    @Test
    void 안_당한_경우_최고점은_100점이다() {
        List<CheckpointResult> allDone = List.of(new CheckpointResult("A", "a", 100, true, 1));
        assertEquals(100, ScoreCalculator.score(ReportResult.SAFE, allDone, null, 10));
    }

    @Test
    void 당하면_당한_턴보다_앞에서_달성한_것만_센다() {
        // 3턴에 당함 → 2턴의 25점만 인정. 0.49 × 25 + 5 = 17.25
        assertEquals(17, ScoreCalculator.score(ReportResult.SCAMMED, CHECKPOINTS, 3, 5));
    }

    @Test
    void 당한_경우_최고점은_49점이다() {
        List<CheckpointResult> allDone = List.of(new CheckpointResult("A", "a", 100, true, 1));
        assertEquals(49, ScoreCalculator.score(ReportResult.SCAMMED, allDone, 5, 10));
    }

    @Test
    void 보정_점수는_0에서_10_사이로_자른다() {
        assertEquals(60, ScoreCalculator.score(ReportResult.SAFE, List.of(), null, 99));
        assertEquals(50, ScoreCalculator.score(ReportResult.SAFE, List.of(), null, -3));
    }
}
