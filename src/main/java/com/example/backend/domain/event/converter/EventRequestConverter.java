package com.example.backend.domain.event.converter;

import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

@Component
public class EventRequestConverter {


    /**
     * CreateFormEvent DTO -> Event 엔티티 변환 메서드
     * @param dto : CreateFormEvent DTO
     * @return EventEntity
     */
    public EventEntity formDtoToEntity(EventRequestDto.FormEvent dto, UserEntity userEntity) {
        return EventEntity.builder()
                .eventTitle(dto.getEventTitle())
                .user(userEntity)
                .eventType(dto.getEventType())
                .lastResponseTime(LocalDateTime.now().minusDays(1))
                .nextPollingAt(LocalDateTime.now().plusMinutes(1))
                .isActive(true)
                .activeUntil(LocalDateTime.now().plusDays(30))
                .build();
    }

    /**
     * CreateFormEvent DTO -> EventMetadata 엔티티 변환 메서드
     * @param dto : CreateFormEvent DTO
     * @return EventMetadataEntity
     */
    public EventMetadataEntity formDtoToMetadataEntity(
            EventRequestDto.FormEvent dto,
            EventEntity eventEntity,
            GoogleFormCreateResponseDto googleFormCreateResponseDto
    ) {
        List<String> questionIds = dto.getSearchColumns().stream()
                .map(column -> googleFormCreateResponseDto.getSearchColumnsIds().get(column))
                .toList();

        return EventMetadataEntity.builder()
                .event(eventEntity)
                .count(0L)
                .searchColumns(dto.getSearchColumns())
                .formId(googleFormCreateResponseDto.getFormId())
                .formUrl(googleFormCreateResponseDto.getFormUrl())
                .searchColumnsIds(questionIds)
                .viewCount(0L)
                .build();
    }

    /**
     * 이벤트 생성 요청 DTO -> 이벤트 엔티티 변환 컨버터
     * @param dto
     * @param userEntity
     * @return
     */
    public EventEntity fileDtoToEntity(EventRequestDto.FileEvent dto, UserEntity userEntity) {
        return EventEntity.builder()
                .eventTitle(dto.getEventTitle())
                .user(userEntity)
                .eventType(dto.getEventType())
                .isActive(true)
                .activeUntil(LocalDateTime.now().plusDays(30))
                .build();
    }

    /**
     * 이벤트 생성 요청 DTO -> 이벤트 메타데이터 엔티티 변환 컨버터
     * @param dto
     * @param eventEntity
     * @return
     */
    public EventMetadataEntity fileDtoToMetadataEntity(EventRequestDto.FileEvent dto, EventEntity eventEntity){
        return EventMetadataEntity.builder()
                .event(eventEntity)
                .count(dto.getCount())
                .searchColumns(dto.getSearchColumns())
                .viewCount(0L)
                .build();
    }
}
