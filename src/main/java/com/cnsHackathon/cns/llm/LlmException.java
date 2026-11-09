package com.cnsHackathon.cns.llm;

/** AI 호출 실패. 서비스 계층에서 LLM_UNAVAILABLE 오류로 바꿔 프론트에 알린다. */
public class LlmException extends RuntimeException {

    private final boolean retryable;
    private final boolean rateLimited;

    public LlmException(String message, boolean retryable) {
        this(message, retryable, false);
    }

    public LlmException(String message, boolean retryable, boolean rateLimited) {
        super(message);
        this.retryable = retryable;
        this.rateLimited = rateLimited;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public boolean isRateLimited() {
        return rateLimited;
    }
}
