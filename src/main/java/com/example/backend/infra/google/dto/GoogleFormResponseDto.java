package com.example.backend.infra.google.dto;

import lombok.*;

import java.util.Map;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GoogleFormResponseDto {
    private String responseId;  // 응답 고유 ID
    private String createTime;  // 응답 제출 시간
    private Map<String, String> answers; // <질문ID, 답변내용>

    public static GoogleFormResponseDto of(String responseId, String createTime, Map<String, String> answers) {
        return GoogleFormResponseDto.builder()
                .responseId(responseId)
                .createTime(createTime)
                .answers(answers)
                .build();
    }
}
