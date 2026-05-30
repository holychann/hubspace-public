package com.example.backend.infra.redis;

import com.example.backend.infra.redis.helper.RedisBatchContext;
import com.example.backend.infra.redis.helper.RedisJsonCache;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractRedisCache<K, V> {

    private final RedisJsonCache redisJsonCache;
    private final String keyPrefix;
    private final String suffix;
    private final TypeReference<V> typeReference;
    private final long defaultTtlSeconds;

    protected AbstractRedisCache(
            RedisJsonCache redisJsonCache,
            String keyPrefix,
            String suffix,
            TypeReference<V> typeReference,
            long defaultTtlSeconds
    ) {
        this.redisJsonCache = redisJsonCache;
        this.keyPrefix = keyPrefix;
        this.suffix = suffix;
        this.typeReference = typeReference;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public V get(K key) {
        return redisJsonCache.get(redisKey(key), typeReference);
    }

    public List<V> multiGet(List<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> redisKeys = keys.stream()
                .map(this::redisKey)
                .toList();

        return redisJsonCache.multiGet(redisKeys, typeReference);
    }

    public Map<K, V> multiGetMap(List<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> redisKeys = keys.stream()
                .map(this::redisKey)
                .toList();

        List<V> values = redisJsonCache.multiGet(redisKeys, typeReference);

        Map<K, V> result = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            V value = values.get(i);
            if (value != null) {
                result.put(keys.get(i), value);
            }
        }
        return result;
    }

    public void put(K key, V value) {
        put(key, value, defaultTtlSeconds);
    }

    public void put(K key, V value, long ttlSeconds) {
        redisJsonCache.put(redisKey(key), value, ttlSeconds);
    }

    public void multiPut(Map<K, V> values) {
        multiPut(values, defaultTtlSeconds);
    }

    public void multiPut(Map<K, V> values, long ttlSeconds) {
        if (values == null || values.isEmpty()) return;

        Map<String, V> redisKeyed = new HashMap<>(values.size());
        for (Map.Entry<K, V> entry : values.entrySet()) {
            redisKeyed.put(redisKey(entry.getKey()), entry.getValue());
        }

        redisJsonCache.multiPut(redisKeyed, ttlSeconds);
    }

    public void putInBatch(RedisBatchContext ctx, K key, V value) {
        putInBatch(ctx, key, value, defaultTtlSeconds);
    }

    public void putInBatch(RedisBatchContext ctx, K key, V value, long ttlSeconds) {
        ctx.put(redisKey(key), value, ttlSeconds);
    }

    public void multiPutInBatch(RedisBatchContext ctx, Map<K, V> values) {
        multiPutInBatch(ctx, values, defaultTtlSeconds);
    }

    public void multiPutInBatch(RedisBatchContext ctx, Map<K, V> values, long ttlSeconds) {
        if (values == null || values.isEmpty()) return;
        for (Map.Entry<K, V> entry : values.entrySet()) {
            ctx.put(redisKey(entry.getKey()), entry.getValue(), ttlSeconds);
        }
    }

    public void evict(K key) {
        redisJsonCache.evict(redisKey(key));
    }

    protected String redisKey(K key) {
        return keyPrefix + ":" + key + ":" + suffix;
    }

    protected Long increment(K key) {
        return redisJsonCache.increment(redisKey(key));
    }

    protected Long increment(K key, long delta) {
        return redisJsonCache.increment(redisKey(key), delta);
    }
}
