package com.example.backend.infra.redis;

import com.example.backend.infra.redis.helper.RedisJsonCache;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

@Component
public class EventSearchColumnsCache extends AbstractRedisCache<Long, SearchColumnsAndEventId> {

    private static final String KEY_PREFIX = "event";
    private static final String SUFFIX = "search_columns";
    private static final int TTL_SECONDS = 60 * 60;

    private static final TypeReference<SearchColumnsAndEventId> SEARCH_COLUMNS = new TypeReference<>() {};

    public EventSearchColumnsCache(RedisJsonCache redisJsonCache) {
        super(redisJsonCache, KEY_PREFIX, SUFFIX, SEARCH_COLUMNS, TTL_SECONDS);
    }
}
