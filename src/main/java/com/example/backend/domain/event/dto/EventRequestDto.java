package com.example.backend.domain.event.dto;

import com.example.backend.domain.event.entity.EventType;
import com.example.backend.global.validation.annotation.EventTitle;
import com.example.backend.global.validation.group.CreateGroup;
import com.example.backend.global.validation.group.UpdateGroup;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EventRequestDto {

    /**
     * Google Form 생성 요청 DTO
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormEvent {
        @EventTitle(groups = {CreateGroup.class, UpdateGroup.class})
        private String eventTitle;

        @NotEmpty(groups = {CreateGroup.class})
        @Null(groups = {UpdateGroup.class})
        private List<String> searchColumns;

        @NotNull(groups = {CreateGroup.class})
        @Null(groups = {UpdateGroup.class})
        private EventType eventType;
    }

    /**
     * CSV, TSC 등 파일 생성 요청 DTO
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileEvent {
        @EventTitle(groups = {CreateGroup.class})
        private String eventTitle;

        @Positive(groups = {CreateGroup.class})
        @NotNull(groups = {CreateGroup.class})
        private Long count;

        @NotEmpty(groups = {CreateGroup.class})
        private List<String> searchColumns;

        @NotNull(groups = {CreateGroup.class})
        private EventType eventType;
    }

    /**
     * CSV, TSC 파일 수정 DTO
     */
    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateFileEvent {
        @EventTitle(groups = UpdateGroup.class)
        private String eventTitle;
    }

}
