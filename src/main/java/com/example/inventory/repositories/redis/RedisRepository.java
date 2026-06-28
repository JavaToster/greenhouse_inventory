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

    public long increment(String key) {
        byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);
        
        Long result = redisTemplate.execute(connection -> 
            connection.stringCommands().incr(rawKey), 
            true
        );
        
        return result != null ? result : 0L;
    }

    public void setExpireInMinutes(String key, long timeoutMinutes) {
        redisTemplate.expire(key, Duration.ofMinutes(timeoutMinutes));
    }

    public <T> T getAndDelete(String key, Class<T> type) {
        if (key == null) {
            return null;
        }

        byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);

        byte[] rawValue = redisTemplate.execute(connection ->
                        connection.stringCommands().getDel(rawKey),
                true
        );

        if (rawValue == null) {
            return null;
        }

        Object value = redisTemplate.getValueSerializer().deserialize(rawValue);

        return type.isInstance(value) ? type.cast(value) : null;
    }
}
