package com.example.backend.domain.event;

import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.domain.user.entity.UserRoleType;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import com.example.backend.infra.redis.dto.EventMetadataCacheDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventTestFixture {

    public static final String USERNAME = "tester";
    public static final String OTHER_USERNAME = "stranger";
    public static final Long EVENT_ID = 1001L;
    public static final Long METADATA_ID = 2001L;

    private EventTestFixture() {}

    public static UserEntity user() {
        return user(USERNAME);
    }

    public static UserEntity user(String username) {
        return UserEntity.builder()
                .id(1L)
                .username(username)
                .password("password")
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER)
                .nickname("nickname")
                .email("user@example.com")
                .build();
    }

    public static EventEntity formEvent() {
        return formEvent(EVENT_ID, user());
    }

    public static EventEntity formEvent(Long id, UserEntity user) {
        return EventEntity.builder()
                .id(id)
                .user(user)
                .eventTitle("타이틀")
                .eventType(EventType.FORM)
                .isActive(true)
                .nextPollingAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    public static EventEntity fileEvent() {
        return EventEntity.builder()
                .id(EVENT_ID)
                .user(user())
                .eventTitle("파일타이틀")
                .eventType(EventType.FILE)
                .isActive(true)
                .build();
    }

    public static EventMetadataEntity metadata() {
        return metadata(0L);
    }

    public static EventMetadataEntity metadata(long count) {
        return EventMetadataEntity.builder()
                .id(METADATA_ID)
                .event(formEvent())
                .count(count)
                .viewCount(0L)
                .searchColumns(List.of("col1", "col2"))
                .searchColumnsIds(List.of("id1", "id2"))
                .formId("form-id")
                .formUrl("https://forms.example.com/form-id")
                .build();
    }

    public static EventRequestDto.FormEvent formRequest() {
        return EventRequestDto.FormEvent.builder()
                .eventTitle("새타이틀")
                .searchColumns(List.of("col1", "col2"))
                .eventType(EventType.FORM)
                .build();
    }

    public static EventRequestDto.FileEvent fileRequest() {
        return EventRequestDto.FileEvent.builder()
                .eventTitle("새파일타이틀")
                .count(10L)
                .searchColumns(List.of("col1", "col2"))
                .eventType(EventType.FILE)
                .build();
    }

    public static EventRequestDto.UpdateFileEvent updateFileRequest(String title) {
        return EventRequestDto.UpdateFileEvent.builder()
                .eventTitle(title)
                .build();
    }

    public static GoogleFormCreateResponseDto googleFormResponse() {
        Map<String, String> ids = new HashMap<>();
        ids.put("col1", "qid-1");
        ids.put("col2", "qid-2");
        return GoogleFormCreateResponseDto.builder()
                .formId("form-id")
                .formUrl("https://forms.example.com/form-id")
                .searchColumnsIds(ids)
                .build();
    }

    public static EventWithMetadataDto eventWithMetadata(Long id, EventType type) {
        return EventWithMetadataDto.builder()
                .eventId(id)
                .username(USERNAME)
                .eventTitle("타이틀-" + id)
                .eventType(type)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .metadataId(id + 1000)
                .count(0L)
                .viewCount(0L)
                .searchColumns(List.of("col1", "col2"))
                .searchColumnsIds(List.of("id1", "id2"))
                .formId("form-" + id)
                .formUrl("https://forms.example.com/form-" + id)
                .build();
    }

    public static EventMetadataCacheDto cacheDto(Long id, EventType type) {
        return EventMetadataCacheDto.builder()
                .eventId(id)
                .username(USERNAME)
                .eventTitle("타이틀-" + id)
                .eventType(type)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .metadataId(id + 1000)
                .searchColumns(List.of("col1", "col2"))
                .searchColumnsIds(List.of("id1", "id2"))
                .formId("form-" + id)
                .formUrl("https://forms.example.com/form-" + id)
                .build();
    }
}
