package com.cnsHackathon.cns.conversation.dto;

import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.global.support.Texts;
import com.cnsHackathon.cns.session.domain.EndReason;
import java.time.Duration;

/**
 * 사기범 발화 한 개. 프론트는 speaker가 B면 다른 목소리로 읽는다.
 * ended가 true면 이 발화를 재생한 뒤 /end 없이 바로 리포트를 요청한다.
 */
public record TurnResponse(
        int turnNo,
        String speaker,
        String text,
        boolean ended,
        EndReason endReason,
        int remainingSec) {

    public static TurnResponse of(Utterance line, EndReason endReason, Duration remaining) {
        return new TurnResponse(
                line.getTurnNo(),
                line.getSpeaker(),
                line.getText(),
                endReason != null,
                endReason,
                Texts.ceilSeconds(remaining));
    }
}
