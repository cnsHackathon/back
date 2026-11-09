package com.cnsHackathon.cns.session.dto;

import com.cnsHackathon.cns.session.domain.EndReason;
import jakarta.validation.constraints.NotNull;

/** reason은 HANG_UP 또는 TIMEOUT. */
public record EndSessionRequest(@NotNull EndReason reason) {
}
