package com.cnsHackathon.cns.session.dto;

import com.cnsHackathon.cns.session.domain.EndReason;

/** endReason은 실제로 기록된 종료 사유다. 이미 끝난 통화면 처음 사유가 그대로 온다. */
public record EndSessionResponse(boolean ended, EndReason endReason) {
}
