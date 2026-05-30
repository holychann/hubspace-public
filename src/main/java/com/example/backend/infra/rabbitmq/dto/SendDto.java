package com.example.backend.infra.rabbitmq.dto;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendDto {
    private String username;
    private String formId;
    private Long eventId;
    private List<String> searchColumnIds;
    private LocalDateTime lastResponseTime;
    private LocalDateTime createdAt;

    public static SendDto of(EventWithMetadataDto eventWithMetadataDto) {
        return SendDto.builder()
                .eventId(eventWithMetadataDto.getEventId())
                .username(eventWithMetadataDto.getUsername())
                .formId(eventWithMetadataDto.getFormId())
                .searchColumnIds(eventWithMetadataDto.getSearchColumnsIds())
                .lastResponseTime(eventWithMetadataDto.getLastResponseTime())
                .createdAt(eventWithMetadataDto.getCreatedAt())
                .build();
    }
}
