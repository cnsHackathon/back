package com.cnsHackathon.cns.scenario;

/** 사기 세부 유형. 이름은 프론트의 관련 사기 유형 표시와 맞춰 둔다. */
public enum ScamType {

    INSTITUTION("기관 사칭", ScamCategory.IMPERSONATION),
    FAMILY_KIDNAP("자녀 납치 빙자", ScamCategory.IMPERSONATION),
    FAMILY_CHILD("자녀 사칭", ScamCategory.IMPERSONATION),
    FAMILY_SPOUSE("배우자 사고 빙자", ScamCategory.IMPERSONATION),
    LOAN("대출 빙자", ScamCategory.IMPERSONATION),
    NOSHOW_PROXY_PURCHASE("노쇼·대리구매", ScamCategory.THIRD_PARTY);

    private final String label;
    private final ScamCategory category;

    ScamType(String label, ScamCategory category) {
        this.label = label;
        this.category = category;
    }

    public String label() {
        return label;
    }

    public ScamCategory category() {
        return category;
    }
}
