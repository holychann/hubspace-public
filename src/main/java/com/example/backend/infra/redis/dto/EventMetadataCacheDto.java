package com.example.backend.infra.redis.dto;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventMetadataCacheDto {
    private Long eventId;
    private String username;
    private String eventTitle;
    private EventType eventType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime nextPollingAt;
    private LocalDateTime lastPollingAt;
    private LocalDateTime lastResponseTime;
    private Long metadataId;
    private List<String> searchColumns;
    private List<String> searchColumnsIds;
    private String formId;
    private String formUrl;

    public static EventMetadataCacheDto from(EventEntity event, EventMetadataEntity metadata) {
        return EventMetadataCacheDto.builder()
                .eventId(event.getId())
                .username(event.getUser().getUsername())
                .eventTitle(event.getEventTitle())
                .eventType(event.getEventType())
                .isActive(event.getIsActive())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .nextPollingAt(event.getNextPollingAt())
                .lastPollingAt(event.getLastPollingAt())
                .lastResponseTime(event.getLastResponseTime())
                .metadataId(metadata.getId())
                .searchColumns(metadata.getSearchColumns())
                .searchColumnsIds(metadata.getSearchColumnsIds())
                .formId(metadata.getFormId())
                .formUrl(metadata.getFormUrl())
                .build();
    }

    public static EventMetadataCacheDto from(EventWithMetadataDto event) {
        return EventMetadataCacheDto.builder()
                .eventId(event.getEventId())
                .username(event.getUsername())
                .eventTitle(event.getEventTitle())
                .eventType(event.getEventType())
                .isActive(event.getIsActive())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .nextPollingAt(event.getNextPollingAt())
                .lastPollingAt(event.getLastPollingAt())
                .lastResponseTime(event.getLastResponseTime())
                .metadataId(event.getMetadataId())
                .searchColumns(event.getSearchColumns())
                .searchColumnsIds(event.getSearchColumnsIds())
                .formId(event.getFormId())
                .formUrl(event.getFormUrl())
                .build();
    }


}
