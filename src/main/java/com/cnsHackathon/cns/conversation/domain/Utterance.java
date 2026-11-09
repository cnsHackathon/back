package com.cnsHackathon.cns.conversation.domain;

import com.cnsHackathon.cns.global.support.Texts;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 통화 중 발화 하나. 턴 하나는 사기범 발화 1개와 그에 대한 사용자 답변 1개다.
 * 사기범 첫 발화가 1턴이고, 사용자 답변은 자신이 답한 사기범 발화와 같은 턴 번호를 쓴다.
 */
@Entity
@Table(name = "utterance", indexes = @Index(name = "idx_utterance_session_seq", columnList = "session_id, seq"))
public class Utterance {

    public static final String USER = "USER";
    public static final int MAX_TEXT_LENGTH = 2000;
    private static final int MAX_NOTE_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    /** 통화 안에서의 순서 (0부터) */
    @Column(nullable = false)
    private int seq;

    @Column(nullable = false)
    private int turnNo;

    /** USER, 또는 사기범 등장인물 키(C, B) */
    @Column(nullable = false, length = 10)
    private String speaker;

    @Column(name = "content", nullable = false, length = MAX_TEXT_LENGTH)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CallPhase phase;

    /** 사기범 발화를 만들 때 AI가 남긴 판정 메모. 사용자에게는 보이지 않는다. */
    @Column(length = MAX_NOTE_LENGTH)
    private String judgeNote;

    @Column(nullable = false)
    private Instant createdAt;

    protected Utterance() {
    }

    public static Utterance user(String sessionId, int seq, int turnNo, String text, Instant at) {
        Utterance utterance = new Utterance();
        utterance.sessionId = sessionId;
        utterance.seq = seq;
        utterance.turnNo = turnNo;
        utterance.speaker = USER;
        utterance.text = Texts.truncate(text, MAX_TEXT_LENGTH);
        utterance.createdAt = at;
        return utterance;
    }

    public static Utterance scammer(String sessionId, int seq, int turnNo, String speaker, String text,
                                    CallPhase phase, String judgeNote, Instant at) {
        Utterance utterance = new Utterance();
        utterance.sessionId = sessionId;
        utterance.seq = seq;
        utterance.turnNo = turnNo;
        utterance.speaker = speaker;
        utterance.text = Texts.truncate(text, MAX_TEXT_LENGTH);
        utterance.phase = phase;
        utterance.judgeNote = Texts.truncate(judgeNote, MAX_NOTE_LENGTH);
        utterance.createdAt = at;
        return utterance;
    }

    public boolean isUser() {
        return USER.equals(speaker);
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getSeq() {
        return seq;
    }

    public int getTurnNo() {
        return turnNo;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public CallPhase getPhase() {
        return phase;
    }

    public String getJudgeNote() {
        return judgeNote;
    }
}
