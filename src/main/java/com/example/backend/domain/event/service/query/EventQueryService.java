package com.example.backend.domain.event.service.query;

import com.example.backend.domain.event.dto.EventDetail;
import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

public interface EventQueryService {
    SearchList getEventList(String username);
    EventDetail getEventDetail(String username, Long eventId);
    IsActive getEventIsActive(Long eventId);
    SearchColumnsAndEventId getEventColumns(Long eventId);
    List<EventWithMetadataDto> getNextPollingEvents(LocalDateTime threshold);
    List<String> getEventColumnIds(Long eventId);
    EventType getEventType(Long eventId);
    List<Long> getInactiveEventIds();
}
