package com.example.backend.infra.redis.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisJsonCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T get(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            redisTemplate.delete(key);
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> typeRef) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;

        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            redisTemplate.delete(key);
            return null;
        }
    }

    public <T> List<T> multiGet(List<String> keys, TypeReference<T> typeRef) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> jsons = redisTemplate.opsForValue().multiGet(keys);

        List<T> result = new ArrayList<>(keys.size());
        if (jsons == null) {
            for (int i = 0; i < keys.size(); i++) {
                result.add(null);
            }
            return result;
        }

        for (int i = 0; i < jsons.size(); i++) {
            String json = jsons.get(i);
            if (json == null) {
                result.add(null);
                continue;
            }
            try {
                result.add(objectMapper.readValue(json, typeRef));
            } catch (Exception e) {
                redisTemplate.delete(keys.get(i));
                result.add(null);
            }
        }
        return result;
    }

    public void put(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            // ignore
        }
    }

    public <T> void multiPut(Map<String, T> values, long ttlSeconds) {
        if (values == null || values.isEmpty()) return;

        Map<String, String> serialized = new HashMap<>(values.size());
        for (Map.Entry<String, T> entry : values.entrySet()) {
            try {
                serialized.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            } catch (Exception e) {
                // ignore
            }
        }
        if (serialized.isEmpty()) return;

        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                for (Map.Entry<String, String> entry : serialized.entrySet()) {
                    ops.opsForValue().set(entry.getKey(), entry.getValue(), ttlSeconds, TimeUnit.SECONDS);
                }
                return null;
            }
        });
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }
}
