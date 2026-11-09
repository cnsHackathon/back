package com.cnsHackathon.cns.global.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ApiErrorResponse.of(code, e.getMessage()));
    }

    /** JSON 형식이 깨졌거나 "30대"처럼 정해진 값이 아닌 경우. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        log.debug("요청 본문을 읽지 못했어요: {}", e.getMessage());
        return respond(ErrorCode.INVALID_REQUEST);
    }

    /** 같은 통화를 동시에 고치려 한 경우. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(OptimisticLockingFailureException e) {
        return respond(ErrorCode.TURN_IN_PROGRESS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOthers(Exception e) {
        // 검증 실패(@Valid), 없는 주소 같은 Spring 기본 예외는 상태 코드를 그대로 쓴다.
        if (e instanceof org.springframework.web.ErrorResponse framework) {
            HttpStatusCode status = framework.getStatusCode();
            ErrorCode code = status.value() == 404 ? ErrorCode.NOT_FOUND
                    : status.is4xxClientError() ? ErrorCode.INVALID_REQUEST
                    : ErrorCode.INTERNAL_ERROR;
            return ResponseEntity.status(status).body(ApiErrorResponse.of(code));
        }
        log.error("처리하지 못한 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> respond(ErrorCode code) {
        return ResponseEntity.status(code.getStatus()).body(ApiErrorResponse.of(code));
    }
}
