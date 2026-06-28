package com.example.inventory.repositories.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    public <T> T findByKey(String key, Class<T> type){
        Object value = redisTemplate.opsForValue().get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void saveWithTTLInSeconds(String key, Object value, long TTLSeconds){
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(TTLSeconds));
    }

    public void remove(String key){
        redisTemplate.delete(key);
    }

    public void saveWithTTLInMinutes(String key, Object value, long TTLMinutes){
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(TTLMinutes));
    }

    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }
}
