package com.cnsHackathon.cns.conversation;

import com.cnsHackathon.cns.conversation.domain.CallPhase;
import java.time.Duration;

/**
 * 사기범 발화를 만들 때 알려 주는 진행 상황.
 *
 * @param lastTurn 시간이 다 돼서 이번 발화로 통화를 마무리해야 하면 true
 */
public record CallContext(Duration elapsed, Duration remaining, boolean lastTurn) {

    public static CallContext opening(Duration timeLimit) {
        return new CallContext(Duration.ZERO, timeLimit, false);
    }

    public CallPhase recommendedPhase() {
        return CallPhase.at(elapsed);
    }
}
