package com.grid.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StringRedisTemplate stringRedisTemplate;

    public void handleBotNotification(Long userId, String botName) {
        String cooldownKey = "user:" + userId + ":notif_cooldown";
        Boolean cooldownExists = stringRedisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(cooldownExists)) {
            String pendingKey = "user:" + userId + ":pending_notifs";
            stringRedisTemplate.opsForList().rightPush(
                    pendingKey,
                    "Bot " + botName + " replied to your post"
            );
        } else {
            log.info("Push Notification Sent to User {}", userId);
            stringRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(900));
        }
    }
}
