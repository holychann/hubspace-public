package com.example.backend.domain.event.controller;

import com.example.backend.domain.event.service.command.EventCommandService;
import com.example.backend.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/events")
@Slf4j
public class EventCommandController {

    private final EventCommandService eventCommandService;

    @PostMapping("{eventId}/close")
    public ResponseEntity<ApiResponseDto> closeEvent(
            @AuthenticationPrincipal String username,
            @PathVariable("eventId") Long id
    ) {

        log.info("🔄[EVENT][CTRL][CLOSE][START] 이벤트 종료처리 | eventId: {}", id);

        long start = System.currentTimeMillis();

        eventCommandService.closeEvent(username, id);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("🆗[EVENT][CTRL][CLOSE][END] 이벤트 종료처리 완료 | eventId: {}, 걸린시간: {}초", id, elapsed);

        return ResponseEntity.ok(ApiResponseDto.success());
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponseDto> deleteFormEvent(
            @AuthenticationPrincipal String username,
            @PathVariable("eventId") Long id
    ) {

        log.info("🔄[EVENT][CTRL][DELETE][START] 폼 이벤트 삭제 | eventId: {}", id);

        long start = System.currentTimeMillis();

        eventCommandService.deleteEvent(username, id);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("🆗[EVENT][CTRL][DELETE][END] 폼 이벤트 삭제 완료 | eventId: {}, 걸린시간: {}초", id, elapsed);

        return ResponseEntity.ok(ApiResponseDto.success());
    }

}
