package com.example.backend.domain.response.controller;

import com.example.backend.application.facade.ResponseFacade;
import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1")
public class ResponseQueryController {

    private final ResponseFacade responseFacade;

    /**
     * 답변 조회
     * @param params : 답변 조회 조건
     * @return ApiResponseDto
     */
    @GetMapping("/events/{eventId}/search")
    public ResponseEntity<ApiResponseDto> getResponse(
            @PathVariable String eventId,
            @RequestParam Map<String, String> params
            ){

        long start = System.currentTimeMillis();

        log.info("🔎[RESPONSE][CTRL][GET][START] 응답 데이터 조회 | eventId: {}, params: {}", eventId, params);

        // DTO로 변환
        ResponseDto queryDto = ResponseDto.of(Long.parseLong(eventId), params);

        ResponseDto response = responseFacade.getResponse(queryDto);

        long end = System.currentTimeMillis();
        Double elapsed = (double) (end - start) / 1000.0;

        log.info("🔎[RESPONSE][CTRL][GET][END] 응답 데이터 조회 완료 | 걸린시간: {}초", elapsed);

        return ResponseEntity.ok(ApiResponseDto.success(response));

    }

}
