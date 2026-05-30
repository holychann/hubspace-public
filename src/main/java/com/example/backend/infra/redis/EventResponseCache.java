package com.example.backend.infra.redis;

import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.infra.redis.helper.RedisJsonCache;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventResponseCache extends AbstractRedisCache<Long, List<ResponseDto>> {

    private static final String KEY_PREFIX = "event";
    private static final String SUFFIX = "responses";
    private static final int TTL_SECONDS = 60 * 60;

    private static final TypeReference<List<ResponseDto>> RESPONSE_LIST = new TypeReference<>() {};

    public EventResponseCache(RedisJsonCache redisJsonCache) {
        super(redisJsonCache, KEY_PREFIX, SUFFIX, RESPONSE_LIST, TTL_SECONDS);
    }
}
