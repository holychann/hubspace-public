package com.example.backend.infra.google.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleFormCreateResponseDto {
    private String formId;
    private String formUrl;
    private Map<String, String> searchColumnsIds;

    public static GoogleFormCreateResponseDto of(String formId, String formUrl, GoogleFormQuestionsIdsResponseDto questionsIds) {
        return GoogleFormCreateResponseDto.builder()
                .formId(formId)
                .formUrl(formUrl)
                .searchColumnsIds(questionsIds.getQuestionsIds())
                .build();
    }
}
