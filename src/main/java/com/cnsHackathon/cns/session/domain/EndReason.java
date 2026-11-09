package com.cnsHackathon.cns.session.domain;

public enum EndReason {

    SCAMMED("사용자가 사기범의 요구에 응해서 통화가 끝났다."),
    DETECTED("사용자가 사기를 알아차리고 거절해서 사기범이 통화를 끝냈다."),
    HANG_UP("사용자가 통화를 끊었다."),
    TIMEOUT("통화 시간이 다 돼서 끝났다.");

    private final String description;

    EndReason(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    /** 프론트가 /end로 보낼 수 있는 사유. 나머지는 서버가 판정한다. */
    public boolean isClientReason() {
        return this == HANG_UP || this == TIMEOUT;
    }
}
