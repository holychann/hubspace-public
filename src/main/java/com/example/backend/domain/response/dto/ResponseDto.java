package com.example.backend.domain.response.dto;

import com.example.backend.domain.response.entity.ResponseEntity;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 데이터 조회 요청 DTO 겸 데이터 조회 응답 반환 DTO
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseDto {
    private Long eventId;
    private Map<String, String> answers;

    public static ResponseDto of(ResponseEntity entity) {
        return ResponseDto.builder()
                .eventId(entity.getEventId())
                .answers(entity.getAnswers() == null ? null : new LinkedHashMap<>(entity.getAnswers()))
                .build();
    }

    public static ResponseDto of(Long eventId, Map<String, String> answers) {
        return ResponseDto.builder()
                .eventId(eventId)
                .answers(answers == null ? null : new LinkedHashMap<>(answers))
                .build();
    }
}
