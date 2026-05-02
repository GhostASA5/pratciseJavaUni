package com.example.cachepractice.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisJsonCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisJsonCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        String value = redis.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize value for key=" + key, e);
        }
    }

    public void delete(String key) {
        redis.delete(key);
    }
}

