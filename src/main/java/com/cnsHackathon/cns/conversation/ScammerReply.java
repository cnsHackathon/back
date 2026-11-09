package com.cnsHackathon.cns.conversation;

import com.cnsHackathon.cns.conversation.domain.CallPhase;
import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.global.support.Texts;
import com.cnsHackathon.cns.llm.JsonSchemas;
import com.cnsHackathon.cns.session.domain.EndReason;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI가 매 턴 돌려주는 사기범 발화와 판정.
 *
 * @param endReason 통화를 끝내는 이유. 끝내지 않으면 NONE
 * @param judgeNote 사용자에게 보이지 않는 판정 메모
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScammerReply(
        String speaker,
        String utterance,
        CallPhase phase,
        Boolean ended,
        Verdict endReason,
        String judgeNote) {

    // Jackson 3는 빠지거나 null인 boolean을 오류로 처리하므로 Boolean으로 받고 false로 채운다.
    public ScammerReply {
        ended = Boolean.TRUE.equals(ended);
    }

    /** "C: 안녕하세요"처럼 화자 표시를 붙여 오는 경우를 지운다. */
    private static final Pattern SPEAKER_PREFIX = Pattern.compile("^\\s*\\[?[A-Z]]?\\s*[:：]\\s*");

    public enum Verdict {
        NONE,
        SCAMMED,
        DETECTED
    }

    public static Map<String, Object> schema(Collection<String> speakers) {
        return JsonSchemas.object(JsonSchemas.props(
                "speaker", JsonSchemas.enumOf(speakers, "지금 말하는 등장인물의 키"),
                "utterance", JsonSchemas.string("사기범이 이번에 할 말. 전화 통화처럼 1~3문장, 120자 이내"),
                "phase", JsonSchemas.enumOf(CallPhase.values(), "이번 발화의 진행 단계"),
                "ended", JsonSchemas.bool("이번 발화로 통화를 끝내면 true"),
                "endReason", JsonSchemas.enumOf(Verdict.values(), "통화를 끝내는 이유. 끝내지 않으면 NONE"),
                "judgeNote", JsonSchemas.string("사용자의 직전 답변에 대한 판정 메모 한 문장")));
    }

    public boolean isUsable(Collection<String> speakers) {
        return Texts.hasText(utterance) && speakers.contains(speaker) && phase != null && endReason != null;
    }

    /**
     * 통화를 끝낼지 정한다. null이면 통화가 이어진다.
     * 규칙과 달리 판정 없이 ended=true만 온 경우는 사용자가 강하게 거절해 사기범이 물러난 상황으로 본다.
     */
    public EndReason toEndReason(boolean lastTurn) {
        return switch (endReason) {
            case SCAMMED -> EndReason.SCAMMED;
            case DETECTED -> EndReason.DETECTED;
            case NONE -> lastTurn ? EndReason.TIMEOUT : ended ? EndReason.DETECTED : null;
        };
    }

    public Utterance toUtterance(String sessionId, int seq, int turnNo, Instant at) {
        String text = SPEAKER_PREFIX.matcher(utterance.trim()).replaceFirst("").trim();
        return Utterance.scammer(sessionId, seq, turnNo, speaker, text, phase, judgeNote, at);
    }
}
