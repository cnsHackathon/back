package com.cnsHackathon.cns.session.dto;

import com.cnsHackathon.cns.global.support.Texts;
import com.cnsHackathon.cns.session.domain.CallSession;
import java.time.Duration;
import java.time.Instant;

/**
 * 시각은 프론트가 Date.parse로 읽을 수 있게 ISO-8601 문자열로 보낸다. (예: 2026-11-09T08:00:00Z)
 */
public record StartSessionResponse(String startedAt, String deadlineAt, int remainingSec) {

    public static StartSessionResponse of(CallSession session, Instant now, Duration timeLimit) {
        return new StartSessionResponse(
                session.getStartedAt().toString(),
                session.getDeadlineAt().toString(),
                Texts.ceilSeconds(session.remaining(now, timeLimit)));
    }
}
