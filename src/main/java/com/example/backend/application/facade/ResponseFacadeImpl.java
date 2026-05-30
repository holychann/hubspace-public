package com.example.backend.application.facade;

import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.event.service.command.EventCommandService;
import com.example.backend.domain.event.service.query.EventQueryService;
import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.domain.response.service.query.ResponseQueryService;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.backend.application.facade.helper.ResponseHelper.extracted;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseFacadeImpl implements ResponseFacade{

    private final EventQueryService eventQueryService;
    private final ResponseQueryService responseQueryService;
    private final EventCommandService eventCommandService;

    /**
     * 답변 조회
     * @param queryDto : 조회할 답변 정보
     * @return ResponseQueryDto
     */
    @Override
    public ResponseDto getResponse(ResponseDto queryDto) {

        log.info("🗒️[RESPONSE][FACADE][GET] 답변 조회 요청 | queryDto: {}", queryDto);
        /**
         * 해당 메서드 역할 :
         * ✅ 이벤트 존재 여부 확인
         * ✅ 이벤트 활성화 여부 확인
         * ✅ 이벤트 검색 컬럼 조회
         * ❗ 확인이 안되면 BusinessException 발생
         */
        EventResponseDto.SearchColumnsAndEventId eventColumns = eventQueryService.getEventColumns(queryDto.getEventId());

        log.info("✏️[RESPONSE][FACADE] 컬럼 조회: {}", eventColumns.getSearchColumns());

        // 검색 컬럼과 답변 데이터 키가 일치하는지 확인
        if(!extracted(queryDto, eventColumns)){
            log.error("❌ [RESPONSE][FACADE][GET] 검색 컬럼과 답변 데이터 키가 일치하지 않습니다.");
            throw new BusinessException(ErrorCode.DATA_INVALID_INPUT_VALUE);
        }

        EventType eventType = eventQueryService.getEventType(queryDto.getEventId());


        ResponseDto response;

        if(eventType == EventType.FILE){
            response = responseQueryService.getResponse(queryDto.getEventId(), queryDto.getAnswers());
            response = ResponseDto.of(
                    queryDto.getEventId(),
                    orderBySearchColumns(response.getAnswers(), eventColumns.getSearchColumns())
            );
        } else {
            List<String> eventColumnIds = eventQueryService.getEventColumnIds(queryDto.getEventId());

            Map<String, String> formAnswerMap = columnNamesToColumnIds(queryDto, eventColumnIds, eventColumns);

            response = responseQueryService.getResponse(queryDto.getEventId(), formAnswerMap);

            Map<String, String> formAnswerMapWithColumnNames = columnIdsToColumnNames(response.getAnswers(), eventColumnIds, eventColumns);

            response = ResponseDto.of(queryDto.getEventId(), formAnswerMapWithColumnNames);
        }

        log.info("✏️[RESPONSE][FACADE] 최종 컬럼 조회: {}", response.getAnswers());

        log.info("✏️[RESPONSE][FACADE][GET] 데이터 조회하는데에 걸린시간: {}");


        // view count 1 늘리기
        eventCommandService.addViewCount(queryDto.getEventId());

        return response;
    }

    /**
     * 컬럼 이름을 컬럼 아이디로 바꾸는 메서드
     * @param queryDto
     * @param eventColumnIds
     * @param eventColumns
     * @return "컬럼ID": "답변 내용" 으로 이루어진 MAP
     */
    private static Map<String, String> columnNamesToColumnIds(ResponseDto queryDto, List<String> eventColumnIds, EventResponseDto.SearchColumnsAndEventId eventColumns) {
        Map<String, String> formAnswerMap = new LinkedHashMap<>();

        // 컬럼명 -> 컬럼ID 로 스위치
        for(int i = 0; i < eventColumnIds.size(); i++){
            formAnswerMap.put(eventColumnIds.get(i), queryDto.getAnswers().get(eventColumns.getSearchColumns().get(i)));
        }
        return formAnswerMap;
    }

    /**
     * 컬럼 아이디를 컬럼 이름으로 바꾸는 메서드 (역방향)
     * @param answerMap "컬럼ID": "답변 내용"
     * @param eventColumnIds 컬럼 ID 리스트
     * @param eventColumns 이벤트 컬럼 정보 (컬럼 이름 포함)
     * @return "컬럼 이름": "답변 내용" 으로 이루어진 MAP
     */
    private static Map<String, String> columnIdsToColumnNames(
            Map<String, String> answerMap,
            List<String> eventColumnIds,
            EventResponseDto.SearchColumnsAndEventId eventColumns
    ) {
        Map<String, String> resultMap = new LinkedHashMap<>();

        for (int i = 0; i < eventColumnIds.size(); i++) {
            String columnId = eventColumnIds.get(i);
            String columnName = eventColumns.getSearchColumns().get(i);

            if (answerMap.containsKey(columnId)) {
                resultMap.put(columnName, answerMap.get(columnId));
            }
        }

        return resultMap;
    }

    private static Map<String, String> orderBySearchColumns(Map<String, String> originalMap, List<String> searchColumns) {
        Map<String, String> orderedMap = new LinkedHashMap<>();

        if (originalMap == null) {
            return orderedMap;
        }

        for (String column : searchColumns) {
            if (originalMap.containsKey(column)) {
                orderedMap.put(column, originalMap.get(column));
            }
        }

        return orderedMap;
    }


}
