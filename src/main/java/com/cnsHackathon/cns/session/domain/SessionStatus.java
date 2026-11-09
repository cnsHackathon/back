package com.cnsHackathon.cns.session.domain;

public enum SessionStatus {
    /** 전화가 울리는 중 (사기범 첫 발화까지 준비됨) */
    CREATED,
    /** 받기를 눌러 통화 중 */
    IN_PROGRESS,
    ENDED
}
