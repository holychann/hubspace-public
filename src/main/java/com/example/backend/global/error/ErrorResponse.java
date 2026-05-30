package com.example.backend.global.error;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 클라이언트에게 반환되는 표준 에러 응답 DTO.
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class ErrorResponse {

    private final String errorCode;
    private final String errorMessage;

    public ErrorResponse(ErrorCode errorCode) {
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode);
    }
}