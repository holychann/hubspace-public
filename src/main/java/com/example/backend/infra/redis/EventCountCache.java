package com.example.backend.infra.redis;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.infra.redis.helper.RedisBatchContext;
import com.example.backend.infra.redis.helper.RedisJsonCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EventCountCache extends AbstractRedisCache<Long, Long> {

    private static final String KEY_PREFIX = "event";
    private static final String SUFFIX = "count";
    private static final int TTL_SECONDS = 60 * 60;
    private static final TypeReference<Long> EVENT_COUNT =
            new TypeReference<>() {};

    public EventCountCache(RedisJsonCache redisJsonCache) {
        super(redisJsonCache, KEY_PREFIX, SUFFIX, EVENT_COUNT, TTL_SECONDS);
    }

    public void put(Long eventId, Long count) {
        if (eventId == null || count == null) {
            return;
        }
        super.put(eventId, count);
    }

    public void putAll(List<EventWithMetadataDto> eventData) {

        if (eventData == null || eventData.isEmpty()) {
            return;
        }

        Map<Long, Long> cacheMap = eventData.stream()
                .filter(event -> event != null
                        && event.getEventId() != null
                        && event.getCount() != null)
                .collect(Collectors.toMap(
                        EventWithMetadataDto::getEventId,
                        EventWithMetadataDto::getCount,
                        (existing, replacement) -> replacement
                ));
        if (cacheMap.isEmpty()) {
            return;
        }
        multiPut(cacheMap);
    }

    public void putAllInBatch(RedisBatchContext ctx, List<EventWithMetadataDto> eventData) {
        if (eventData == null || eventData.isEmpty()) {
            return;
        }
        Map<Long, Long> cacheMap = eventData.stream()
                .filter(event -> event != null
                        && event.getEventId() != null
                        && event.getCount() != null)
                .collect(Collectors.toMap(
                        EventWithMetadataDto::getEventId,
                        EventWithMetadataDto::getCount,
                        (existing, replacement) -> replacement
                ));
        if (cacheMap.isEmpty()) {
            return;
        }
        multiPutInBatch(ctx, cacheMap);
    }

    public Map<Long, Long> getCounts(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return multiGetMap(eventIds);
    }

    public Long increment(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return super.increment(eventId, 1L);
    }

    public Long increment(Long eventId, long delta) {
        if (eventId == null) {
            return null;
        }
        return super.increment(eventId, delta);
    }
}