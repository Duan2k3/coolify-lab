package dev.lab.coolify.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TaskCacheTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private TaskCache taskCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        taskCache = new TaskCache(redisTemplate, objectMapper, true, Duration.ofMinutes(5));
    }

    @Test
    void readsCachedTask() throws Exception {
        TaskResponse task = taskResponse();
        when(valueOperations.get("task:" + task.id())).thenReturn(objectMapper.writeValueAsString(task));

        assertThat(taskCache.get(task.id())).contains(task);
    }

    @Test
    void fallsBackToCacheMissWhenRedisIsUnavailable() {
        UUID id = UUID.randomUUID();
        when(valueOperations.get("task:" + id))
            .thenThrow(new RedisConnectionFailureException("offline"));

        assertThat(taskCache.get(id)).isEmpty();
    }

    @Test
    void writesTaskWithConfiguredTtl() {
        TaskResponse task = taskResponse();

        taskCache.put(task);

        verify(valueOperations).set(
            eq("task:" + task.id()),
            anyString(),
            eq(Duration.ofMinutes(5))
        );
    }

    private TaskResponse taskResponse() {
        Instant now = Instant.parse("2026-10-28T03:30:00Z");
        return new TaskResponse(UUID.randomUUID(), "DEMO", "cached", TaskStatus.PENDING, now, now);
    }
}
