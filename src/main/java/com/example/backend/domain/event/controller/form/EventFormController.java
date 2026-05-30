package com.example.backend.domain.event.controller.form;

import com.example.backend.application.facade.EventFacade;
import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.event.service.command.EventCommandService;
import com.example.backend.global.dto.ApiResponseDto;
import com.example.backend.global.validation.group.CreateGroup;
import com.example.backend.global.validation.group.UpdateGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventFormController {

    private final EventFacade eventFacade;
    private final EventCommandService eventCommandService;

    @PostMapping("/form")
    public ResponseEntity<ApiResponseDto> createFormEvent(
            @AuthenticationPrincipal String username,
            @RequestBody @Validated(CreateGroup.class) EventRequestDto.FormEvent eventRequestDto
    ) {
        log.info("🆕[EVENT][CTRL][CREATE][START] 폼 이벤트 생성 | username: {}", username);

        long start = System.currentTimeMillis();

        EventResponseDto.CreatedFormEvent formInfo = eventFacade.createFormEvent(username, eventRequestDto);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("🆗[EVENT][CTRL][CREATE][END] 폼 이벤트 생성 완료 | eventId: {}, 걸린시간: {}초", formInfo.getEventId(), elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(formInfo));
    }

    @PatchMapping("/{eventId}/form")
    public ResponseEntity<ApiResponseDto> updateFormEvent(
            @AuthenticationPrincipal String username,
            @PathVariable("eventId") Long id,
            @RequestBody @Validated(UpdateGroup.class) EventRequestDto.FormEvent eventRequestDto
    ) {
        log.info("🔄[EVENT][CTRL][UPDATE][START] 폼 이벤트 수정 | eventId: {}", id);

        long start = System.currentTimeMillis();

        EventResponseDto.Update update = eventCommandService.updateFormEvent(username, id, eventRequestDto);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("🆗[EVENT][CTRL][UPDATE][END] 폼 이벤트 수정 완료 | eventId: {}, 걸린시간: {}초", id, elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(update));
    }

}
