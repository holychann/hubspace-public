package com.example.backend.infra.redis.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisOperations;

import java.util.concurrent.TimeUnit;

public class RedisBatchContext {

    private final RedisOperations<String, String> ops;
    private final ObjectMapper objectMapper;

    RedisBatchContext(RedisOperations<String, String> ops, ObjectMapper objectMapper) {
        this.ops = ops;
        this.objectMapper = objectMapper;
    }

    public void put(String key, Object value, long ttlSeconds) {
        try {
            ops.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            // ignore
        }
    }
}
