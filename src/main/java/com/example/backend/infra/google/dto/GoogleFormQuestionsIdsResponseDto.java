package com.example.backend.infra.google.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleFormQuestionsIdsResponseDto {
    Map<String, String> questionsIds;

    public static GoogleFormQuestionsIdsResponseDto of(Map<String, String> questionsIds) {
        return GoogleFormQuestionsIdsResponseDto.builder()
                .questionsIds(questionsIds)
                .build();
    }
}
