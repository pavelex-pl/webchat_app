package com.webchat.presence;

import java.time.Duration;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    static final Duration TAB_TTL = Duration.ofSeconds(120);
    static final long ONLINE_WINDOW_MS = 60_000;
    static final long STALE_AFTER_MS = 120_000;

    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static String tabsKey(long userId) { return "presence:user:" + userId; }
    private static String statusKey(long userId) { return "presence:status:" + userId; }
    private static final String DIRTY_KEY = "presence:dirty";

    public void recordActivity(long userId, String tabId, long lastActivityAtMs) {
        redis.opsForZSet().add(tabsKey(userId), tabId, lastActivityAtMs);
        redis.expire(tabsKey(userId), TAB_TTL);
        redis.opsForSet().add(DIRTY_KEY, String.valueOf(userId));
    }

    public void removeTab(long userId, String tabId) {
        redis.opsForZSet().remove(tabsKey(userId), tabId);
        redis.opsForSet().add(DIRTY_KEY, String.valueOf(userId));
    }

    public Status compute(long userId) {
        long now = System.currentTimeMillis();
        String key = tabsKey(userId);
        // prune stale entries first
        redis.opsForZSet().removeRangeByScore(key, 0, now - STALE_AFTER_MS);
        Long onlineCount = redis.opsForZSet().count(key, now - ONLINE_WINDOW_MS, Double.POSITIVE_INFINITY);
        if (onlineCount != null && onlineCount > 0) return Status.ONLINE;
        Long total = redis.opsForZSet().zCard(key);
        if (total != null && total > 0) return Status.AFK;
        return Status.OFFLINE;
    }

    public Set<String> drainDirty() {
        Set<String> all = redis.opsForSet().members(DIRTY_KEY);
        if (all != null && !all.isEmpty()) redis.delete(DIRTY_KEY);
        return all;
    }

    public Status cachedStatus(long userId) {
        String v = redis.opsForValue().get(statusKey(userId));
        return v == null ? Status.OFFLINE : Status.valueOf(v);
    }

    public void setCachedStatus(long userId, Status status) {
        if (status == Status.OFFLINE) redis.delete(statusKey(userId));
        else redis.opsForValue().set(statusKey(userId), status.name());
    }
}
