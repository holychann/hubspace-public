package com.example.backend.domain.event.service.query;

import com.example.backend.domain.event.converter.EventResponseConverter;
import com.example.backend.domain.event.dto.EventDetail;
import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.event.repository.query.EventMetadataQueryRepository;
import com.example.backend.domain.event.repository.query.EventQueryRepository;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.redis.*;
import com.example.backend.infra.redis.dto.EventMetadataCacheDto;
import com.example.backend.infra.redis.helper.RedisPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

/**
 * 이벤트 Query Service 구현 클래스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventQueryServiceImpl implements EventQueryService{

    private final EventQueryRepository eventQueryRepository;
    private final EventResponseConverter eventResponseConverter;
    private final EventMetadataQueryRepository eventMetadataQueryRepository;
    private final EventSearchColumnsCache eventSearchColumnsCache;
    private final EventMetadataCache eventMetadataCache;
    private final EventUserEventIdsCache eventUserEventIdsCache;
    private final EventViewCountCache eventViewCountCache;
    private final EventCountCache eventCountCache;
    private final RedisPipeline redisPipeline;

    /**
     * 사용자의 진행중인 이벤트 리스트 조회
     * @param username : 사용자 유저네임
     * @return SearchList
     */
    @Override
    @Transactional(readOnly = true)
    public SearchList getEventList(String username) {

        List<EventWithMetadataDto> eventData = null;
        List<Long> eventIds = eventUserEventIdsCache.get(username);
        Map<Long, EventMetadataCacheDto> cachedEvents;

        // 캐시가 존재한다면
        if(eventIds != null && !eventIds.isEmpty()){
            cachedEvents = eventMetadataCache.multiGetMap(eventIds);

            if(cachedEvents.size() == eventIds.size()){
                log.info("⚡️[CACHE][HIT] 이벤트 리스트 | username={}", username);

                Map<Long, Long> counts = eventCountCache.getCounts(eventIds);
                Map<Long, Long> viewCounts = eventViewCountCache.getViewCounts(eventIds);

                eventData = eventIds.stream()
                        .map(eventId -> EventWithMetadataDto.from(
                                cachedEvents.get(eventId),
                                counts.getOrDefault(eventId, 0L),
                                viewCounts.getOrDefault(eventId, 0L)
                        ))
                        .toList();
            }
        }
        else {
            log.info("🦆[CACHE][MISS] 이벤트 리스트 | username={}", username);

            eventData = eventQueryRepository.findByUserIdAndIsActive(username, true);

            long start = System.nanoTime();

            List<Long> ids = eventData.stream().map(EventWithMetadataDto::getEventId).toList();
            List<EventWithMetadataDto> finalEventData = eventData;
            redisPipeline.execute(ctx -> {
                eventMetadataCache.putAllInBatch(ctx, finalEventData);
                eventUserEventIdsCache.putInBatch(ctx, username, ids);
                eventCountCache.putAllInBatch(ctx, finalEventData);
                eventViewCountCache.putAllInBatch(ctx, finalEventData);
            });

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("Redis Response time: {}ms", elapsedMs);
        }

        log.info("📋[EVENT][SERVICE][QUERY][OK] 이벤트 리스트 조회 | count: {}", eventData.size());

        return eventResponseConverter.toSearchListDto(eventData);
    }


    /**
     * 이벤트 상세 정보 조회
     * @param username : 사용자 정보
     * @param eventId : 이벤트 ID
     * @return EventDetail
     */
    @Override
    @Transactional(readOnly = true)
    public EventDetail getEventDetail(String username, Long eventId) {

        EventWithMetadataDto eventData = eventQueryRepository.findByUserIdAndEventIdAndIsActive(username, eventId, true);

        log.info("🆗[EVENT][SERVICE][QUERY][OK] 이벤트 상세 정보 조회 | eventId: {}", eventId);

        if(eventData.getEventType() == EventType.FORM){
            return eventResponseConverter.toSearchFormDto(eventData);
        } else if (eventData.getEventType() == EventType.FILE){
            return eventResponseConverter.toSearchFileDto(eventData);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 이벤트 활성화 여부 조회
     * @param eventId : 이벤트 ID
     * @return Boolean
     */
    @Override
    @Transactional(readOnly = true)
    public IsActive getEventIsActive(Long eventId) {

        Boolean result = eventQueryRepository.existsById(eventId);

        log.info("🆗[EVENT][SERVICE][QUERY][OK] 이벤트 상태 조회 | eventId: {}, isActive: {}", eventId, result);

        return eventResponseConverter.toIsActiveDto(eventId, result);
    }

    /**
     * 이벤트 검색 컬럼 및 이벤트 타이틀 조회
     * @param eventId : 이벤트 ID
     * @return SearchColumnsAndEventId
     */
    @Override
    @Transactional(readOnly = true)
    public SearchColumnsAndEventId getEventColumns(Long eventId) {


        SearchColumnsAndEventId cached = null;


        // 캐시 조회
        cached = eventSearchColumnsCache.get(eventId);

        // 있으면
        if(cached != null){
            log.info("⚡️[CACHE][HIT] eventId={}", eventId);
            return cached;
        }

        log.info("🦆️[CACHE][MISS] eventId={}", eventId);

        // 이벤트 조회 | 존재하지 않으면 예외 발생
        EventWithMetadataDto eventWithMetadata = eventQueryRepository.findByEventIdAndIsActive(eventId, true)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));


        SearchColumnsAndEventId result = eventResponseConverter.toSearchColumnsAndEventIdDto(eventWithMetadata);


        // Redis 캐싱
        eventSearchColumnsCache.put(eventId, result);

        log.info("🆗[EVENT][SERVICE][QUERY][OK] 이벤트 검색용 컬럼 조회 | eventId: {}", eventId);

        return result;
    }

    /**
     * 다음 검색 이벤트들 조회
     * @param threshold
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventWithMetadataDto> getNextPollingEvents(LocalDateTime threshold) {
        List<EventWithMetadataDto> events = eventQueryRepository.findByNextPollingAtBefore(threshold);
        log.info("🔎[EVENT][SERVICE][QUERY][OK] 다음 폴링 대상 이벤트들 조회 | count: {}", events.size());
        return events;
    }

    @Override
    public List<String> getEventColumnIds(Long eventId) {
        EventMetadataEntity metadata = eventMetadataQueryRepository.findByEventId(eventId);

        return metadata.getSearchColumnsIds();
    }

    @Override
    public EventType getEventType(Long eventId) {

        EventEntity eventEntity = eventQueryRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        return eventEntity.getEventType();
    }

    /**
     * 비활성화 되어야하는 이벤트들의 아이디를 반환하는 함수.
     * 현재 시간과 activeUntil 을 비교한다.
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public List<Long> getInactiveEventIds(){
        LocalDateTime now = LocalDateTime.now();

        return eventQueryRepository.findInactiveEventIds(now);
    }
}
