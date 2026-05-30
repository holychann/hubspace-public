package com.example.backend.domain.response.service.command;

import com.example.backend.domain.event.controller.helper.EventHelper;
import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.domain.response.helper.DynamoBatchUtil;
import com.example.backend.domain.response.entity.ResponseEntity;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.google.dto.GoogleFormResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseCommandServiceImpl implements ResponseCommandService{

    private final DynamoDbTable<ResponseEntity> responseTable;
    private final DynamoBatchUtil dynamoBatchUtil;
    private final EventHelper eventHelper;

    private static final int TTL_DAYS = 35;

    @Override
    public void saveFormResponses(List<GoogleFormResponseDto> responses, Long eventId) {

        log.info("💾[RESPONSE][SERVICE][SAVE] 응답 저장 시작 | 응답 개수: {}, eventId:{}", responses.size(), eventId);

        Long ttl = calculateTTL();

        try{
            // 1. 변환 로직 수행
            List<ResponseEntity> responseEntities = ResponseEntity.fromGoogleFormList(responses, eventId, ttl);

            // 2. 분리된 유틸리티를 사용하여 저장
            dynamoBatchUtil.batchWrite(responseEntities, ResponseEntity.class, responseTable);

        } catch(Exception e){
            // 무슨 에러가 발생할지 몰라 INTERNAL 에러 발생시킴. 나중에 확인 후 바꿔야함.
            log.error("❌[RESPONSE][SERVICE][SAVE][ERROR] 응답 저장 중 에러 발생 | 메시지: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * CSV 파일의 데이터 저장
     *
     * @param file
     * @param eventId
     */
    @Override
    public List<ResponseDto> saveFileResponses(MultipartFile file, List<String> searchColumns, Long eventId) {
        log.info("💾[RESPONSE][SERVICE][SAVE] FILE rows 저장 시작 | eventId:{}", eventId);

        List<ResponseEntity> responseEntities;

        try {

            List<Map<String, String>> parsedRows = eventHelper.fileParser(file, searchColumns);

            Long ttl = calculateTTL();

            // 1. DTO -> Entity 변환
            responseEntities = ResponseEntity.fromFileList(parsedRows, eventId, ttl);

            // 2. 저장
            dynamoBatchUtil.batchWrite(responseEntities, ResponseEntity.class, responseTable);

        } catch (Exception e) {
            // 무슨 에러가 발생할지 몰라 INTERNAL 에러 발생시킴. 나중에 확인 후 바꿔야함.
            log.error("❌[RESPONSE][SERVICE][SAVE][ERROR] 데이터 저장 중 에러 발생 | 메시지: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return responseEntities.stream()
                .map(ResponseDto::of)
                .toList();

    }

    /**
     * 데이터의 TTL 을 계산하는 메서드
     * @return
     */
    private static Long calculateTTL() {
        return Instant.now()
                .plus(TTL_DAYS, ChronoUnit.DAYS)
                .getEpochSecond();
    }
}
