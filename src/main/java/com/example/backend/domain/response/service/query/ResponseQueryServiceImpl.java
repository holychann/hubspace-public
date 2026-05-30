package com.example.backend.domain.response.service.query;

import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.domain.response.entity.ResponseEntity;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.redis.EventResponseCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;

import static com.example.backend.domain.response.helper.MatchAnswersUtil.findMatchedResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseQueryServiceImpl implements ResponseQueryService{

    private final DynamoDbTable<ResponseEntity> responseTable;
    private final EventResponseCache eventResponseCache;

    /**
     * 응답을 조회하는 메서드
     * @param eventId : 이벤트 아이디
     * @param answersWithSearchColumnIds : 문답
     * @return 조회된 응답 정보
     */
    @Override
    public ResponseDto getResponse(Long eventId, Map<String, String> answersWithSearchColumnIds) {

        if (answersWithSearchColumnIds == null || answersWithSearchColumnIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_QUERY);
        }

        List<ResponseDto> cachedResponses = null;


        cachedResponses = eventResponseCache.get(eventId);


        ResponseDto matched = null;

        if (cachedResponses != null) {
            log.info("⚡️[CACHE][HIT] eventId={}", eventId);

            matched = findMatchedResponse(cachedResponses, answersWithSearchColumnIds);
        }

        // 캐시가 있는데 데이터가 없거나, 캐시가 없거나
        if (matched == null) {
            log.info("💾[CACHE][FALLBACK] eventId={} → DynamoDB 조회", eventId);

            PageIterable<ResponseEntity> result =
                    responseTable.query(
                            QueryConditional.keyEqualTo(k -> k.partitionValue(eventId))
                    );

            List<ResponseEntity> items = new ArrayList<>();
            result.forEach(page -> items.addAll(page.items()));

            List<ResponseDto> responses = items.stream()
                    .map(ResponseDto::of)
                    .toList();

                // Redis 캐싱
            eventResponseCache.put(eventId, responses, 3600);

            matched = findMatchedResponse(responses, answersWithSearchColumnIds);
        }


        if (matched == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "응답 데이터 없음");
        }

        log.info("🗒️[RESPONSE][SERVICE][QUERY][OK] 응답 조회 결과 | responseData: {}", matched.toString());

        return matched;
    }

    @Override
    public List<ResponseDto> getResponses(Long eventId) {

        PageIterable<ResponseEntity> result =
                responseTable.query(QueryConditional.keyEqualTo(k -> k.partitionValue(eventId)));

        List<ResponseEntity> items = new ArrayList<>();
        result.forEach(page -> items.addAll(page.items()));


        return items.stream().map(ResponseDto::of).limit(10).toList();
    }
}
