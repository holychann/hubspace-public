package com.example.backend.domain.event.dto;

import com.example.backend.domain.event.entity.EventType;

import java.time.LocalDateTime;
import java.util.List;

public interface EventDetail {
    String getId();
    Long getCount();
    String getEventTitle();
    Boolean getIsActive();
    EventType getEventType();
    List<String> getSearchColumns();
    LocalDateTime getCreatedAt();
    Long getViewCount();
}
