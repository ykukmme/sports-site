package com.esports.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void okReturnsSuccessTrue() {
        // given
        String data = "테스트 데이터";

        // when
        ApiResponse<String> response = ApiResponse.ok(data);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void failReturnsSuccessFalse() {
        // given
        String errorMessage = "오류가 발생했습니다.";

        // when
        ApiResponse<String> response = ApiResponse.fail(errorMessage);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(errorMessage);
    }
}
