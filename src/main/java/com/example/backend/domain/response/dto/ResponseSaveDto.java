package com.example.backend.domain.response.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseSaveDto {
    private Long eventId;

    public static ResponseSaveDto of(Long eventId){
        return ResponseSaveDto.builder()
                .eventId(eventId)
                .build();
    }
}
