package com.esports.common.exception;

import org.springframework.http.HttpStatus;

// 비즈니스 로직 예외 — 컨트롤러 어드바이스에서 클라이언트 오류(4xx)로 변환
public class BusinessException extends RuntimeException {

    private final String errorCode;
    // HTTP 상태 코드 — 기본값 400, 필요 시 404/403/409 등으로 지정
    private final HttpStatus httpStatus;

    // 기본 생성자 (400 Bad Request)
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    // HTTP 상태 코드 지정 생성자
    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
