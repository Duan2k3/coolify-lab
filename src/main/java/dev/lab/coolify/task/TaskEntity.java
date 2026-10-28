package dev.lab.coolify.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TaskEntity() {
    }

    public TaskEntity(String type, String payload, String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.payload = payload;
        this.idempotencyKey = idempotencyKey;
        this.status = TaskStatus.PENDING;
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = TaskStatus.PENDING;
        }
        createdAt = now;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
