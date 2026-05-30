package com.example.backend.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드 정의.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    /* 400 BAD_REQUEST */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-400", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "허용되지 않는 HTTP 메소드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류입니다."),

    /* 데이터 */
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "DATA-404", "조회 결과가 없습니다."),
    DATA_INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "DATA-400", "잘못된 입력 값입니다."),

    /* 이벤트 */
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT-404", "이벤트가 존재하지 않습니다."),
    EVENT_INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "EVENT-400", "유효한 값을 입력해 주세요."),
    EVENT_FORBIDDEN(HttpStatus.FORBIDDEN, "EVENT-403", "해당 이벤트에 대한 권한이 없습니다."),
    EVENT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EVENT-500", "이벤트 생성 중 오류가 발생하였습니다."),

    /* 구글 */
    GOOGLE_INVALID_GRANT(HttpStatus.BAD_REQUEST, "GOOGLE-400", "유효하지 않는 값입니다."),
    GOOGLE_SECURITY_ERROR(HttpStatus.BAD_REQUEST, "GOOGLE-401", "Google 서비스 보안 연결 설정 중 오류가 발생하였습니다."),
    GOOGLE_API_ERROR(HttpStatus.BAD_GATEWAY, "GOOGLE-500", "Google API 호출 중 오류가 발생하였습니다."),

    /* 응답 */
    INVALID_QUERY(HttpStatus.NOT_FOUND, "RESPONSE-404", "응답이 존재하지 않습니다.");

    private final HttpStatus httpStatus;  // HTTP 상태코드
    private final String code;  // 에러 코드
    private final String message;  // 에러 메시지

}