package com.esports.common;

import com.fasterxml.jackson.annotation.JsonInclude;

// 모든 REST API 응답에 사용되는 공통 래퍼 클래스
// null 필드는 JSON 직렬화에서 제외 (불필요한 null 키 방지)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    // 오류 코드 — 클라이언트에서 오류 종류를 구분할 때 사용
    private final String errorCode;

    private ApiResponse(boolean success, T data, String message, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    // 성공 응답 생성
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    // 실패 응답 생성 (오류 코드 없음)
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    // 실패 응답 생성 (오류 코드 포함)
    public static <T> ApiResponse<T> fail(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
