package com.example.backend.domain.event.controller.file;

import com.example.backend.application.facade.EventFacade;
import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.event.service.command.file.EventFileCommandService;
import com.example.backend.domain.response.dto.ResponseSaveDto;
import com.example.backend.global.dto.ApiResponseDto;
import com.example.backend.global.validation.group.CreateGroup;
import com.example.backend.global.validation.group.UpdateGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventFileController {

    private final EventFacade eventFacade;
    private final EventFileCommandService eventFileCommandService;

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDto<ResponseSaveDto>> createFileEvent(
            @AuthenticationPrincipal String username,
            @RequestPart("file") MultipartFile file,
            @Validated(CreateGroup.class)
            @RequestPart("request") EventRequestDto.FileEvent eventRequestDto
    ){
        log.info("➡️[EVENT][CTRL][CREATE][START] FILE 이벤트 생성 요청 | username: {}", username);

        long start = System.currentTimeMillis();

        ResponseSaveDto result = eventFacade.createFileEvent(username, eventRequestDto, file);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("🆗[EVENT][CTRL][CREATE][END] FILE 이벤트 생성 성공 | eventId: {}, 걸린시간: {}초", result.getEventId(), elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(result));
    }

    /**
     * FILE 이벤트 rows 를 포함한 전체 데이터 수정 요청
     * ⚠️FILE 이벤트는 rows 를 포함하여 데이터를 수정할 수 없음
     *
     * TODO: 추후 기능 추가 예정
     *
     * @param username : 유저 정보
     * @param eventId : 이벤트 아이디
     * @param eventRequestDto : 업데이트 요청 DTO
     * @return
     */
//    @PutMapping("/{eventId}/file")
//    public ResponseEntity<ApiResponseDto> updateNewFileEvent(
//            @AuthenticationPrincipal String username,
//            @PathVariable Long eventId,
//            @RequestBody EventRequestDto.FileEvent eventRequestDto
//    ) {
//        log.info("🔄[EVENT][CTRL][UPDATE][START] FILE 이벤트 전체 수정 요청 | eventId: {}", eventId);
//
//        long start = System.currentTimeMillis();
//
//        EventResponseDto.UpdateFileEvent result = eventFacade.updateFileEvent(username, eventId, eventRequestDto);
//
//        long end = System.currentTimeMillis();
//        Double elapsed = (double) (end - start) / 1000.0;
//
//        log.info("🆗[EVENT][CTRL][UPDATE][END] FILE 이벤트 수정 성공 | eventId: {}, 걸린시간: {}초", result.getEventId(), elapsed);
//
//        return ResponseEntity.ok(ApiResponseDto.success(result));
//    }

    /**
     * FILE 이벤트 간단 업데이트 메서드. title 및 display column 만 업데이트 가능
     *
     * @param username : 유저 정보
     * @param eventId : 이벤트 아이디
     * @param eventRequestDto : 업데이트 요청 DTO
     * @return
     */
    @PatchMapping("/{eventId}/file")
    public ResponseEntity<ApiResponseDto<EventResponseDto.UpdateFileEvent>> simpleUpdateFileEvent(
            @AuthenticationPrincipal String username,
            @PathVariable Long eventId,
            @Validated(UpdateGroup.class) @RequestBody EventRequestDto.UpdateFileEvent eventRequestDto
    ) {
        log.info("🔄[EVENT][CTRL][UPDATE][START] FILE 이벤트 부분 수정 요청 | eventId: {}", eventId);

        long start = System.currentTimeMillis();

        EventResponseDto.UpdateFileEvent result = eventFileCommandService.simpleUpdateFileEvent(username, eventId, eventRequestDto);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("🆗[EVENT][CTRL][UPDATE][END] FILE 이벤트 부분 수정 성공 | eventId: {}, 걸린시간: {}초", result.getEventId(), elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(result));
    }
}
