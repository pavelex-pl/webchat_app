package com.webchat.presence;

import java.time.Duration;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    static final Duration TAB_TTL = Duration.ofSeconds(120);
    static final long ONLINE_WINDOW_MS = 60_000;
    // A tab is considered alive as long as it has pinged within this window.
    // The client pings every 15s, so 60s tolerates two missed pings before
    // the tab is treated as closed (→ user offline if it was their last one).
    static final long HEARTBEAT_GRACE_MS = 60_000;

    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static String heartbeatKey(long userId) { return "presence:heartbeat:" + userId; }
    private static String activityKey(long userId) { return "presence:activity:" + userId; }
    private static String statusKey(long userId) { return "presence:status:" + userId; }
    private static final String DIRTY_KEY = "presence:dirty";

    public void recordActivity(long userId, String tabId, long lastActivityAtMs) {
        long now = System.currentTimeMillis();
        redis.opsForZSet().add(heartbeatKey(userId), tabId, now);
        redis.opsForZSet().add(activityKey(userId), tabId, lastActivityAtMs);
        redis.expire(heartbeatKey(userId), TAB_TTL);
        redis.expire(activityKey(userId), TAB_TTL);
        redis.opsForSet().add(DIRTY_KEY, String.valueOf(userId));
    }

    public void removeTab(long userId, String tabId) {
        redis.opsForZSet().remove(heartbeatKey(userId), tabId);
        redis.opsForZSet().remove(activityKey(userId), tabId);
        redis.opsForSet().add(DIRTY_KEY, String.valueOf(userId));
    }

    public Status compute(long userId) {
        long now = System.currentTimeMillis();
        String hb = heartbeatKey(userId);
        String act = activityKey(userId);
        // Drop tabs whose last heartbeat is too old to still be considered open.
        Set<String> deadTabs = redis.opsForZSet().rangeByScore(hb, 0, now - HEARTBEAT_GRACE_MS);
        if (deadTabs != null && !deadTabs.isEmpty()) {
            redis.opsForZSet().remove(hb, deadTabs.toArray());
            redis.opsForZSet().remove(act, deadTabs.toArray());
        }
        Long aliveTabs = redis.opsForZSet().zCard(hb);
        if (aliveTabs == null || aliveTabs == 0) return Status.OFFLINE;
        // Any alive tab with recent user activity → ONLINE, otherwise AFK.
        Long recentActivity = redis.opsForZSet().count(act, now - ONLINE_WINDOW_MS, Double.POSITIVE_INFINITY);
        if (recentActivity != null && recentActivity > 0) return Status.ONLINE;
        return Status.AFK;
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
