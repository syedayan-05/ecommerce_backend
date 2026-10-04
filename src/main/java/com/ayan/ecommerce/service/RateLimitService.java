package com.ayan.ecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final RedisTemplate<String ,String> redisTemplate;

    public boolean isAllowed(String key,
                             int maxRequests,
                             Duration window){
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(key,window);
        }

        return count <= maxRequests;
    }
    public long incrementAndGet(String key,Duration window){
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            throw new IllegalStateException("Failed to increment Redis counter");
        }

        if (count == 1) {
            redisTemplate.expire(key,window);
        }

        return count;
    }

    public void reset(String key){
        redisTemplate.delete(key);
    }


}
