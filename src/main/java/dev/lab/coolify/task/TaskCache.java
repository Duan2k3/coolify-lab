package dev.lab.coolify.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class TaskCache {

    private static final Logger log = LoggerFactory.getLogger(TaskCache.class);
    private static final String KEY_PREFIX = "task:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration ttl;

    TaskCache(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        @Value("${app.cache.enabled:true}") boolean enabled,
        @Value("${app.cache.task-ttl:PT5M}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttl = ttl;
    }

    Optional<TaskResponse> get(UUID id) {
        if (!enabled) {
            return Optional.empty();
        }
        String key = key(id);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, TaskResponse.class));
        } catch (JsonProcessingException exception) {
            log.warn("Discarding invalid cached task value for {}", id);
            delete(id);
            return Optional.empty();
        } catch (DataAccessException exception) {
            log.warn("Redis read failed; falling back to PostgreSQL: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    void put(TaskResponse task) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(task.id()), objectMapper.writeValueAsString(task), ttl);
        } catch (JsonProcessingException | DataAccessException exception) {
            log.warn("Redis write failed; continuing without cache: {}", exception.getMessage());
        }
    }

    void delete(UUID id) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.delete(key(id));
        } catch (DataAccessException exception) {
            log.warn("Redis eviction failed for task {}: {}", id, exception.getMessage());
        }
    }

    private String key(UUID id) {
        return KEY_PREFIX + id;
    }
}
