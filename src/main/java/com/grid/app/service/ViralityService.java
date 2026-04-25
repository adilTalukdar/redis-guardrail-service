package com.grid.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViralityService {

    private final StringRedisTemplate stringRedisTemplate;

    public Long updateViralityScore(Long postId, String interactionType) {
        String key = "post:" + postId + ":virality_score";
        long delta = switch (interactionType) {
            case "BOT_REPLY" -> 1L;
            case "HUMAN_LIKE" -> 20L;
            case "HUMAN_COMMENT" -> 50L;
            default -> throw new IllegalArgumentException("Unknown interaction type: " + interactionType);
        };
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }
}
