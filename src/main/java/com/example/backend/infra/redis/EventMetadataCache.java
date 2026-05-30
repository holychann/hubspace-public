package com.example.backend.infra.redis;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.infra.redis.dto.EventMetadataCacheDto;
import com.example.backend.infra.redis.helper.RedisBatchContext;
import com.example.backend.infra.redis.helper.RedisJsonCache;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventMetadataCache extends AbstractRedisCache<Long, EventMetadataCacheDto> {

    private static final String KEY_PREFIX = "event";
    private static final String SUFFIX = "metadata";
    private static final int TTL_SECONDS = 60 * 60;

    private static final TypeReference<EventMetadataCacheDto> EVENT_LIST = new TypeReference<>() {};

    public EventMetadataCache(RedisJsonCache redisJsonCache) {
        super(redisJsonCache, KEY_PREFIX, SUFFIX, EVENT_LIST, TTL_SECONDS);
    }

    public void put(EventMetadataCacheDto cacheDto) {
        if (cacheDto == null || cacheDto.getEventId() == null) {
            return;
        }
        super.put(cacheDto.getEventId(), cacheDto);
    }

    public void put(EventEntity event, EventMetadataEntity metadata) {
        if (event == null || metadata == null) {
            return;
        }
        EventMetadataCacheDto cacheDto = EventMetadataCacheDto.from(event, metadata);
        super.put(cacheDto.getEventId(), cacheDto);
    }

    public void putAll(List<EventWithMetadataDto> eventData) {
        if (eventData == null || eventData.isEmpty()) {
            return;
        }
        Map<Long, EventMetadataCacheDto> cacheMap = eventData.stream()
                .filter(event -> event != null && event.getEventId() != null)
                .collect(Collectors.toMap(
                        EventWithMetadataDto::getEventId,
                        EventMetadataCacheDto::from,
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
        Map<Long, EventMetadataCacheDto> cacheMap = eventData.stream()
                .filter(event -> event != null && event.getEventId() != null)
                .collect(Collectors.toMap(
                        EventWithMetadataDto::getEventId,
                        EventMetadataCacheDto::from,
                        (existing, replacement) -> replacement
                ));
        if (cacheMap.isEmpty()) {
            return;
        }
        multiPutInBatch(ctx, cacheMap);
    }
}
