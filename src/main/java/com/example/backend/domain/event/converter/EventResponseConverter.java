package com.example.backend.domain.event.converter;

import com.example.backend.domain.event.dto.EventDetail;
import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

/**
 * 이벤트 Response 용 DTO 변환 클래스
 */
@Component
public class EventResponseConverter {

    /**
     * 모든 이벤트의 Entity -> DTO 변환 메서드
     * @return SearchList
     */
    public SearchList toSearchListDto(List<EventWithMetadataDto> eventList) {

        List<EventDetail> results = eventList.stream()
                .map(dto -> {
                    if (dto.getEventType() == EventType.FORM) {
                        return SearchFormDetail.builder()
                                .id(String.valueOf(dto.getEventId()))
                                .eventTitle(dto.getEventTitle())
                                .eventType(dto.getEventType())
                                .isActive(dto.getIsActive())
                                .createdAt(dto.getCreatedAt())
                                .searchColumns(dto.getSearchColumns())
                                .count(dto.getCount())
                                .viewCount(dto.getViewCount())
                                .formUrl(dto.getFormUrl())
                                .build();
                    }

                    return SearchFileDetail.builder()
                            .id(String.valueOf(dto.getEventId()))
                            .eventTitle(dto.getEventTitle())
                            .isActive(dto.getIsActive())
                            .eventType(dto.getEventType())
                            .searchColumns(dto.getSearchColumns())
                            .createdAt(dto.getCreatedAt())
                            .count(dto.getCount())
                            .viewCount(dto.getViewCount())
                            .build();
                })
                .toList();

        return SearchList.builder()
                .count(eventList.size())
                .events(results)
                .build();

    }

    /**
     * Google Form형 이벤트의 Entity -> DTO 변환 메서드
     * @return SearchFormDetail
     */
    public SearchFormDetail toSearchFormDto(EventWithMetadataDto event) {
        return SearchFormDetail.builder()
                .id(String.valueOf(event.getEventId()))
                .eventTitle(event.getEventTitle())
                .eventType(event.getEventType())
                .isActive(event.getIsActive())
                .createdAt(event.getCreatedAt())
                .searchColumns(event.getSearchColumns())
                .count(event.getCount())
                .viewCount(event.getViewCount())
                .formUrl(event.getFormUrl())
                .build();
    }

    /**
     * CSV형 이벤트의 Entity -> DTO 변환 메서드
     * @return SearchFileDetail
     */
    public SearchFileDetail toSearchFileDto(EventWithMetadataDto event) {
        return SearchFileDetail.builder()
                .id(String.valueOf(event.getEventId()))
                .eventTitle(event.getEventTitle())
                .isActive(event.getIsActive())
                .eventType(event.getEventType())
                .searchColumns(event.getSearchColumns())
                .createdAt(event.getCreatedAt())
                .count(event.getCount())
                .viewCount(event.getViewCount())
                .build();
    }

    public FileDetailController toFileDetailController(EventDetail eventDetail, List<ResponseDto> responseDtos) {
        return FileDetailController.builder()
                .id(eventDetail.getId())
                .eventTitle(eventDetail.getEventTitle())
                .isActive(eventDetail.getIsActive())
                .eventType(eventDetail.getEventType())
                .searchColumns(eventDetail.getSearchColumns())
                .createdAt(eventDetail.getCreatedAt())
                .count(eventDetail.getCount())
                .viewCount(eventDetail.getViewCount())
                .answers(responseDtos)
                .build();
    }

    /**
     * 활성화 여부 DTO
     * @param eventId : 이벤트 ID
     * @param isActive : 활성화 여부
     * @return IsActive
     */
    public IsActive toIsActiveDto(Long eventId, Boolean isActive) {
        return IsActive.builder()
                .eventId(eventId)
                .isActive(isActive)
                .build();
    }

    /**
     * 생성된 Google Form 이벤트 DTO
     * @param eventEntity : EventEntity
     * @param googleFormCreateResponseDto : Google Form 생성 결과 DTO
     * @return CreatedFormEvent
     */
    public CreatedFormEvent toCreatedFormEventDto(EventEntity eventEntity, GoogleFormCreateResponseDto googleFormCreateResponseDto) {
        return CreatedFormEvent.builder()
                .eventId(eventEntity.getId())
                .formUrl(googleFormCreateResponseDto.getFormUrl())
                .build();
    }

    /**
     * 이벤트 ID와 검색 열 정보를 담은 DTO
     * @param eventWithMetadataDto : EventWithMetadataDto
     * @return SearchColumnsAndEventId
     */
    public SearchColumnsAndEventId toSearchColumnsAndEventIdDto(EventWithMetadataDto eventWithMetadataDto) {

        return SearchColumnsAndEventId.builder()
                .eventId(eventWithMetadataDto.getEventId())
                .eventTitle(eventWithMetadataDto.getEventTitle())
                .searchColumns(eventWithMetadataDto.getSearchColumns())
                .build();
    }

    /**
     * 업데이트 성공 DTO 로 변환
     * @param eventEntity : EventEntity
     * @return
     */
    public Update toUpdateSuccessDto(EventEntity eventEntity) {
        return Update.builder()
                .eventId(eventEntity.getId())
                .build();
    }

    public CreatedFileEvent entityToCreatedFileEvent(EventEntity eventEntity) {
        return CreatedFileEvent.builder()
                .eventId(eventEntity.getId())
                .build();
    }

    public UpdateFileEvent toUpdateFileEvent(Long eventId) {
        return UpdateFileEvent.builder()
                .eventId(eventId)
                .build();
    }
}
