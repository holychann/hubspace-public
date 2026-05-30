package com.example.backend.domain.event.dto;

import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.infra.redis.EventCountCache;
import com.example.backend.infra.redis.dto.EventMetadataCacheDto;
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
public class EventWithMetadataDto{

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
    private Long count;
    private Long viewCount;
    private List<String> searchColumns;
    private List<String> searchColumnsIds;
    private String formId;
    private String formUrl;

    public static EventWithMetadataDto from(EventMetadataCacheDto cacheDto, Long count, Long viewCount) {
        return EventWithMetadataDto.builder()
                .eventId(cacheDto.getEventId())
                .username(cacheDto.getUsername())
                .eventTitle(cacheDto.getEventTitle())
                .eventType(cacheDto.getEventType())
                .isActive(cacheDto.getIsActive())
                .createdAt(cacheDto.getCreatedAt())
                .updatedAt(cacheDto.getUpdatedAt())
                .nextPollingAt(cacheDto.getNextPollingAt())
                .lastPollingAt(cacheDto.getLastPollingAt())
                .lastResponseTime(cacheDto.getLastResponseTime())
                .metadataId(cacheDto.getMetadataId())
                .count(count)
                .viewCount(viewCount)
                .searchColumns(cacheDto.getSearchColumns())
                .searchColumnsIds(cacheDto.getSearchColumnsIds())
                .formId(cacheDto.getFormId())
                .formUrl(cacheDto.getFormUrl())
                .build();
    }

}