package com.cnsHackathon.cns.global.error;

/** 모든 오류 응답의 형식: { "code": "...", "message": "..." } */
public record ApiErrorResponse(String code, String message) {

    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message) {
        return new ApiErrorResponse(errorCode.name(), message);
    }
}
