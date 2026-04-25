package com.grid.app.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedRate = 300000)
    public void sweepPendingNotifications() {
        Set<String> keys = stringRedisTemplate.keys("user:*:pending_notifs");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String userId = key.split(":")[1];
            List<String> messages = stringRedisTemplate.opsForList().range(key, 0, -1);
            stringRedisTemplate.delete(key);
            if (messages != null && !messages.isEmpty()) {
                log.info("Summarized Push Notification: {} and {} others interacted with your posts.",
                        messages.get(0), messages.size() - 1);
            }
        }
    }
}
