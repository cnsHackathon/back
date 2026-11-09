package com.cnsHackathon.cns.session.domain;

import com.cnsHackathon.cns.global.error.BusinessException;
import com.cnsHackathon.cns.global.error.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSessionRepository extends JpaRepository<CallSession, String> {

    default CallSession getOrThrow(String sessionId) {
        return findById(sessionId).orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}
