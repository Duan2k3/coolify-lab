package dev.lab.coolify.task;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String type,
    String payload,
    TaskStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    static TaskResponse from(TaskEntity task) {
        return new TaskResponse(
            task.getId(),
            task.getType(),
            task.getPayload(),
            task.getStatus(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
