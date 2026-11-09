package com.cnsHackathon.cns.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnsHackathon.cns.conversation.ScammerReply.Verdict;
import com.cnsHackathon.cns.conversation.domain.CallPhase;
import com.cnsHackathon.cns.conversation.domain.Utterance;
import com.cnsHackathon.cns.session.domain.EndReason;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScammerReplyTest {

    @Test
    void 판정에_따라_통화_종료_사유가_정해진다() {
        assertEquals(EndReason.SCAMMED, reply(true, Verdict.SCAMMED).toEndReason(false));
        assertEquals(EndReason.DETECTED, reply(true, Verdict.DETECTED).toEndReason(false));
        assertNull(reply(false, Verdict.NONE).toEndReason(false));
    }

    @Test
    void 마지막_턴은_판정이_없어도_시간_초과로_끝난다() {
        assertEquals(EndReason.TIMEOUT, reply(false, Verdict.NONE).toEndReason(true));
        assertEquals(EndReason.SCAMMED, reply(true, Verdict.SCAMMED).toEndReason(true));
    }

    @Test
    void 판정_없이_끝내겠다고_하면_사기범이_물러난_것으로_본다() {
        assertEquals(EndReason.DETECTED, reply(true, Verdict.NONE).toEndReason(false));
    }

    @Test
    void 화자_표시는_지우고_저장한다() {
        ScammerReply reply = new ScammerReply("B", "B: 담당자 김대리입니다.", CallPhase.DEMAND, false, Verdict.NONE, "메모");

        Utterance line = reply.toUtterance("s", 3, 2, Instant.EPOCH);

        assertEquals("담당자 김대리입니다.", line.getText());
        assertEquals("B", line.getSpeaker());
        assertEquals(2, line.getTurnNo());
    }

    @Test
    void 시나리오에_없는_화자나_빈_대사는_쓸_수_없다() {
        List<String> speakers = List.of("C");
        assertTrue(reply(false, Verdict.NONE).isUsable(speakers));
        assertFalse(new ScammerReply("B", "안녕하세요", CallPhase.APPROACH, false, Verdict.NONE, "").isUsable(speakers));
        assertFalse(new ScammerReply("C", "  ", CallPhase.APPROACH, false, Verdict.NONE, "").isUsable(speakers));
    }

    @Test
    void 경과_시간에_맞는_단계를_고른다() {
        assertEquals(CallPhase.APPROACH, CallPhase.at(Duration.ofSeconds(30)));
        assertEquals(CallPhase.TRUST, CallPhase.at(Duration.ofMinutes(2)));
        assertEquals(CallPhase.DEMAND, CallPhase.at(Duration.ofMinutes(7)));
        assertEquals(CallPhase.CLOSING, CallPhase.at(Duration.ofMinutes(12)));
    }

    private static ScammerReply reply(boolean ended, Verdict verdict) {
        return new ScammerReply("C", "여보세요, 검찰청입니다.", CallPhase.APPROACH, ended, verdict, "메모");
    }
}
