package com.example.backend.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 애플리케이션 전역 예외를 한 곳에서 처리.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * DTO 유효성 검사 예외
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleInvalid(MethodArgumentNotValidException ex) {

        ErrorCode code = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorCode)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT_VALUE);

        log.error("⚠️[EXCEPTION][VALIDATION] DTO 유효성 검사 실패: CODE={} Message={} (발생시간: {})", code.getCode(), code.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code));
    }

    /* ── 메서드 파라미터/PathVariable 검증 실패 ── */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ErrorResponse> handleViolation(ConstraintViolationException ex) {

        ErrorCode code = ex.getConstraintViolations()
                .stream()
                .map(this::toErrorCode)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT_VALUE);

        log.error("메소드 PathVariable 검증 실패: CODE={} Message={} (발생시간: {})", code.getCode(), code.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code));
    }

    /* ---------- 공통 매핑 메서드 ---------- */
    private ErrorCode toErrorCode(FieldError fe) {
        return resolve(fe.getDefaultMessage());
    }

    private ErrorCode toErrorCode(ConstraintViolation<?> v) {
        return resolve(v.getMessageTemplate());
    }

    /** "{INVALID_AMOUNT}" 형태 → enum 상수 변환, 실패 시 기본코드 */
    private ErrorCode resolve(String template) {
        String key = template.replaceAll("[{}]", "");   // INVALID_AMOUNT
        try {
            return ErrorCode.valueOf(key);
        } catch (IllegalArgumentException e) {
            log.debug("알 수 없는 ErrorCode 템플릿: {}", key);
            return ErrorCode.INVALID_INPUT_VALUE;
        }
    }

    /** 1️⃣ 도메인 계층에서 발생한 커스텀 예외 */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {

        log.warn(
                "⚠️ [BUSINESS_EXCEPTION] code={}, message={}, detailMessage={}, exceptionType={}",
                e.getErrorCode(),
                e.getMessage(),
                e.getErrorMessage(),
                e.getClass().getSimpleName()
        );

        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode(), e.getErrorMessage()));
    }

    /** 2️⃣ 잘못된 HTTP Method */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;

        log.error("잘못된 HTTP 메소드 요청: {} (발생시간: {})", e.getMessage(), LocalDateTime.now());

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ErrorResponse.of(code));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "INVALID_PARAMETER");
        body.put("errorMessage", String.format("파라미터 '%s' 값 '%s'를 %s로 변환할 수 없습니다.",
                ex.getName(), ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "요구타입"));

        // Enum이면 허용 가능한 값들도 같이 내려주기
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            Object[] constants = ex.getRequiredType().getEnumConstants();
            List<String> allowed = Arrays.stream(constants)
                    .map(Object::toString)
                    .toList();
            body.put("allowedValues", allowed);
        }

        // 어떤 필드가 문제인지, 거부된 값은 무엇인지
        body.put("field", ex.getName());
        body.put("rejectedValue", ex.getValue());

        return ResponseEntity.badRequest().body(body); // 400
    }

    /** 3️⃣ 알 수 없는 예외—최종 안전망 */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleUnhandledException(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Swagger 관련 요청은 그냥 예외를 다시 던져서 springdoc이 처리하도록 함
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")) {
            throw new RuntimeException(e);
        }


        log.error("Unhandled exception at {}: {}", LocalDateTime.now(), e.getMessage(), e);

        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ErrorResponse.of(code));
    }
}