package com.example.backend.domain.event.repository.query;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventQueryRepositoryDsl {
    List<EventWithMetadataDto> findByUserIdAndIsActive(String username, Boolean isActive);
    EventWithMetadataDto findByUserIdAndEventIdAndIsActive(String username, Long eventId, Boolean isActive);
    Optional<EventWithMetadataDto> findByEventIdAndIsActive(Long eventId, Boolean isActive);
    List<EventWithMetadataDto> findByNextPollingAtBefore(LocalDateTime threshold);
    List<Long> findInactiveEventIds(LocalDateTime now);
}
