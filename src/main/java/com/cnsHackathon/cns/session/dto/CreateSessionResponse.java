package com.cnsHackathon.cns.session.dto;

import com.cnsHackathon.cns.conversation.dto.TurnResponse;

/** firstTurn은 전화 수신 화면에서 받기를 누르자마자 재생할 사기범 첫 발화다. */
public record CreateSessionResponse(String sessionId, int timeLimitSec, TurnResponse firstTurn) {
}
