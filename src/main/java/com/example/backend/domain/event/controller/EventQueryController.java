package com.example.backend.domain.event.controller;

import com.example.backend.application.facade.EventFacade;
import com.example.backend.domain.event.dto.EventDetail;
import com.example.backend.domain.event.service.query.EventQueryService;
import com.example.backend.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/events")
@Slf4j
public class EventQueryController {

    private final EventFacade eventFacade;
    private final EventQueryService eventQueryService;

    /**
     * 사용자의 진행중인 이벤트 리스트 조회
     * @param username : 로그인한 사용자 정보
     * @return SearchList
     */
    @GetMapping()
    public ResponseEntity<ApiResponseDto<SearchList>> getEventList(
            @AuthenticationPrincipal String username
    ){
        long start = System.currentTimeMillis();
        log.info("🔎[EVENT][CTRL][GET][START] 사용자 이벤트 리스트 조회 | username: {}", username);

        SearchList eventList = eventFacade.getEventList(username);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("✅[EVENT][CTRL][GET][END] 사용자 이벤트 리스트 조회 완료 | 걸린시간: {}초", elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(eventList));
    }

    /**
     * 이벤트 상세 정보 조회
     * @param id : 이벤트 ID
     * @param username : 로그인한 사용자 정보
     * @return EventDetail
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponseDto<EventDetail>> searchEvent(
            @PathVariable("eventId") Long id,
            @AuthenticationPrincipal String username
    ){
        long start = System.currentTimeMillis();
        log.info("🔎[EVENT][CTRL][GET][START] 이벤트 상세정보 조회 | eventId: {}, username: {}", id, username);

        EventDetail eventDetail = eventFacade.getEventDetail(username, id);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("✅[EVENT][CTRL][GET][END] 이벤트 상세정보 조회 완료 | eventId: {}, 걸린시간: {}초", id, elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(eventDetail));
    }

    /**
     * 이벤트가 존재하는지 확인
     * @param id : 이벤트 ID
     * @return IsActive
     */
    @GetMapping("/{eventId}/isActive")
    public ResponseEntity<ApiResponseDto<IsActive>> isExistsEvent(
            @PathVariable("eventId") Long id
    ){
        long start = System.currentTimeMillis();

        log.info("🔎[EVENT][CTRL][GET][START] 이벤트 상태 체크 | eventId: {}", id);

        IsActive isActive = eventQueryService.getEventIsActive(id);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("✅[EVENT][CTRL][GET][END] 이벤트 상태 체크 완료 | eventId: {}, status: {}, 걸린시간: {}초",
                isActive.getEventId(), isActive.getIsActive(), elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(isActive));
        
    }

    /**
     * 이벤트의 검색 컬럼 정보 조회
     * @param id : 이벤트 ID
     * @return SearchColumnsAndEventId
     */
    @GetMapping("/{eventId}/summary")
    public ResponseEntity<ApiResponseDto<SearchColumnsAndEventId>> getSearchColumns(
            @PathVariable("eventId") Long id
    ){
        long start = System.currentTimeMillis();
        log.info("🔎[EVENT][CTRL][GET][START] 이벤트의 검색용 컬럼 조회 | eventId: {}", id);

        SearchColumnsAndEventId searchColumns = eventQueryService.getEventColumns(id);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("✅[EVENT][CTRL][GET][END] 이벤트의 검색용 컬럼 조회 완료 | eventId: {}, 걸린시간: {}초", id, elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(searchColumns));
    }

}
