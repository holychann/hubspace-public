package com.example.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Null 값이 포함되지 않도록 설정
public class ApiResponseDto<T> {
    private boolean success;  // 성공 여부
    private T data;  // 데이터 배열
    private String message;

    /**
     * 성공 응답 생성 메서드
     * @param data 응답 데이터
     * @return ApiResponseDto
     * @param <T> 응답 데이터의 DTO 클래스
     */
    public static <T> ApiResponseDto<T> success(T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static ApiResponseDto success(){
        return ApiResponseDto.builder()
                .success(true)
                .build();
    }

    /**
     * 실패 응답 생성 메서드
     * @param message 실패 메시지
     * @return ApiResponseDto
     */
    public static ApiResponseDto<?> fail(String message) {
        return ApiResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }

}
