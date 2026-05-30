package com.example.backend.infra.redis;

import com.example.backend.infra.redis.helper.RedisJsonCache;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventUserEventIdsCache extends AbstractRedisCache<String, List<Long>>{

    private static final String KEY_PREFIX = "event";
    private static final String SUFFIX = "ids";
    private static final int TTL_SECONDS = 60 * 60;

    private static final TypeReference<List<Long>> EVENT_IDS_TYPE = new TypeReference<>() {};

    public EventUserEventIdsCache(RedisJsonCache redisJsonCache) {
        super(redisJsonCache, KEY_PREFIX, SUFFIX, EVENT_IDS_TYPE, TTL_SECONDS);
    }
}
