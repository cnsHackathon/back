package com.cnsHackathon.cns.global.error;

import org.springframework.http.HttpStatus;

/**
 * 프론트는 응답의 message를 화면에 그대로 띄우므로 메시지는 사용자에게 보여 줄 문장으로 쓴다.
 */
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없어요."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "통화 정보를 찾을 수 없어요. 처음부터 다시 시작해 주세요."),
    SESSION_NOT_STARTED(HttpStatus.CONFLICT, "아직 전화를 받지 않았어요."),
    SESSION_ENDED(HttpStatus.CONFLICT, "이미 끝난 통화예요."),
    SESSION_NOT_ENDED(HttpStatus.CONFLICT, "통화가 끝난 뒤에 리포트를 볼 수 있어요."),
    TURN_IN_PROGRESS(HttpStatus.CONFLICT, "이전 답변을 처리하고 있어요. 잠시 후 다시 시도해 주세요."),
    LLM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "상대방의 응답을 만들지 못했어요. 다시 시도해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 생겼어요. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
