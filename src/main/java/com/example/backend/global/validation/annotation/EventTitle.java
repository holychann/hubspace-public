package com.example.backend.global.validation.annotation;

import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented

@NotBlank
@Size(min = 3, max = 20)
public @interface EventTitle {

    String message() default "이벤트 제목은 3자 이상 20자 이하이어야 합니다.";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
