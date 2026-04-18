package com.webchat.health;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public HealthController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "time", Instant.now().toString(),
                "postgres", pingPostgres(),
                "redis", pingRedis()
        );
    }

    private String pingPostgres() {
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1 ? "ok" : "fail";
        } catch (Exception e) {
            return "fail: " + e.getClass().getSimpleName();
        }
    }

    private String pingRedis() {
        try {
            String pong = redis.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pong) ? "ok" : "fail";
        } catch (Exception e) {
            return "fail: " + e.getClass().getSimpleName();
        }
    }
}
