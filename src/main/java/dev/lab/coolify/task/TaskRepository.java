package dev.lab.coolify.task;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    Optional<TaskEntity> findByIdempotencyKey(String idempotencyKey);
}
