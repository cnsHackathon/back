package com.cnsHackathon.cns.report;

import com.cnsHackathon.cns.report.dto.ReportResponse.CheckpointResult;
import java.util.List;

/**
 * 점수 공식 (설계 문서 "피드백과 점수").
 * - 당함:    min(49,  0.49 × S_pre + B)   S_pre = 당한 턴보다 앞에서 달성한 체크포인트 가중치 합
 * - 안 당함: min(100, 50 + 0.4 × S + B)   S = 달성한 체크포인트 가중치 합
 * - B: 프로필 보정 0~10점 (DifficultyTable)
 */
public final class ScoreCalculator {

    public static final int MAX_BONUS = 10;

    private ScoreCalculator() {
    }

    public static int score(ReportResult result, List<CheckpointResult> checkpoints, Integer scammedTurn, int bonus) {
        int profileBonus = Math.max(0, Math.min(MAX_BONUS, bonus));
        if (result == ReportResult.SCAMMED) {
            int beforeScam = checkpoints.stream()
                    .filter(c -> c.achieved() && c.turn() != null && scammedTurn != null && c.turn() < scammedTurn)
                    .mapToInt(CheckpointResult::weight)
                    .sum();
            return (int) Math.min(49, Math.round(0.49 * beforeScam + profileBonus));
        }
        int achieved = checkpoints.stream()
                .filter(CheckpointResult::achieved)
                .mapToInt(CheckpointResult::weight)
                .sum();
        return (int) Math.min(100, Math.round(50 + 0.4 * achieved + profileBonus));
    }
}
