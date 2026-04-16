package com.esports.common.exception;

import com.esports.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionReturns400() {
        // given
        BusinessException ex = new BusinessException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다.");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("경기를 찾을 수 없습니다.");
        assertThat(response.getBody().getErrorCode()).isEqualTo("MATCH_NOT_FOUND");
    }

    @Test
    void handleBusinessExceptionRespectsHttpStatus() {
        // given: 404로 지정된 비즈니스 예외
        BusinessException ex = new BusinessException("TEAM_NOT_FOUND", "팀을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("TEAM_NOT_FOUND");
    }

    @Test
    void handleValidationExceptionReturns400() throws Exception {
        // given: Bean Validation 실패 모의 객체 생성
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "이름은 필수입니다."));
        bindingResult.addError(new FieldError("target", "region", "지역은 필수입니다."));

        MethodParameter methodParameter = new MethodParameter(
                Object.class.getDeclaredMethod("toString"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodArgumentNotValidException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_FAILED");
        // 필드 오류 메시지가 조합되어 포함되는지 확인
        assertThat(response.getBody().getMessage()).contains("이름은 필수입니다.");
        assertThat(response.getBody().getMessage()).contains("지역은 필수입니다.");
    }

    @Test
    void handleGenericExceptionReturns500() {
        // given
        Exception ex = new RuntimeException("예상치 못한 오류");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("서버 오류가 발생했습니다.");
    }
}
