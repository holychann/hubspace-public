package com.example.backend.domain.event.dto;

import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.response.dto.ResponseDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 이벤트 도메인 API의 응답 DTO
 */
public class EventResponseDto {

    /**
     * 활성화 여부
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IsActive{
        private Long eventId;
        private Boolean isActive;
    }

    /**
     * 수정 성공
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Update {
        private Long eventId;
    }

    /**
     * 모든 이벤트 목록
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchList {
        Integer count;
        List<EventDetail> events;
    }

    /**
     * Google Form형 이벤트의 상세 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchFormDetail implements EventDetail{
        // 공통 멤버
        private String id;
        private String eventTitle;
        private Boolean isActive;
        private EventType eventType;
        private List<String> searchColumns;
        private LocalDateTime createdAt;
        private Long count;
        private Long viewCount;

        // Google Form형 이벤트의 멤버
        private String formUrl;
    }

    /**
     * CSV형 이벤트의 상세 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchFileDetail implements EventDetail{
        // 공통 멤버
        private String id;
        private String eventTitle;
        private Boolean isActive;
        private EventType eventType;
        private List<String> searchColumns;
        private LocalDateTime createdAt;
        private Long count;
        private Long viewCount;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileDetailController implements EventDetail{
        private String id;
        private String eventTitle;
        private Boolean isActive;
        private EventType eventType;
        private List<String> searchColumns;
        private LocalDateTime createdAt;
        private Long count;
        private Long viewCount;

        // CSV형 이벤트의 멤버
        private List<ResponseDto> answers;
    }

    /**
     * Google Form 생성 요청 결과
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreatedFormEvent {
        private Long eventId;
        private String formUrl;
    }


    /**
     * 이벤트 ID와 검색 컬럼 조회
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class SearchColumnsAndEventId{
        private Long eventId;
        private String eventTitle;
        private List<String> searchColumns;
    }

    /**
     * 파일 이벤트 생성 응답 결과
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreatedFileEvent {
        private Long eventId;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateFileEvent {
        private Long eventId;
    }
}
