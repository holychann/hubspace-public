package com.example.backend.domain.event.service.command;

import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.backend.domain.event.dto.EventRequestDto.*;
import static com.example.backend.domain.event.dto.EventResponseDto.*;

public interface EventCommandService {

    CreatedFormEvent createFormEvent(
            UserEntity userEntity,
            FormEvent eventRequestDto,
            GoogleFormCreateResponseDto googleFormCreateResponseDto);

    /**
     * 폴링 데이터 저장합니다.
     *
     * @param eventId : 이벤트 아이디
     * @param nextPollingAt : 다음 폴링 시간
     * @param lastResponseTime : 마지막 응답 시간
     * @param count : 총 응답 개수
     */
    void updatePollingData(Long eventId, LocalDateTime nextPollingAt, LocalDateTime lastResponseTime, Long count);

    /**
     * 이벤트의 상태를 변경하는 메소드 입니다.
     *
     * @param eventId : 이벤트 아이디
     * @param isActive : 활성화 여부
     */
    void updateEventStatus(Long eventId, Boolean isActive);

    /**
     * 다음 폴링 시간만 수정합니다.
     *
     * @param eventId : 이벤트 아이디
     * @param nextPollingAt : 다음 폴링 시간
     */
    void updateNextPollingAt(Long eventId, LocalDateTime nextPollingAt);

    /**
     * 존재하는 이벤트를 삭제합니다.
     * @param eventId : 이벤트 아이디
     */
    void deleteEvent(String username, Long eventId);

    Update updateFormEvent(String username, Long eventId, FormEvent eventRequestDto);

    void closeEvent(String username, Long eventId);

    void addViewCount(Long eventId);

    // 스케쥴러용 메서드
    long expireEventIds(List<Long> eventIds);
}
