package dev.lab.coolify.task;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class TaskCreationTransaction {

    private final TaskRepository taskRepository;

    TaskCreationTransaction(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    TaskCreationResult create(CreateTaskRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<TaskEntity> existing = taskRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return replay(existing.get(), request);
            }
        }

        TaskEntity task = taskRepository.saveAndFlush(
            new TaskEntity(request.type(), request.payload(), idempotencyKey)
        );
        return new TaskCreationResult(TaskResponse.from(task), false);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    TaskCreationResult findReplay(String idempotencyKey, CreateTaskRequest request) {
        TaskEntity task = taskRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new IllegalStateException("Concurrent idempotent request was not committed"));
        return replay(task, request);
    }

    private TaskCreationResult replay(TaskEntity existing, CreateTaskRequest request) {
        if (!existing.getType().equals(request.type()) || !existing.getPayload().equals(request.payload())) {
            throw new IdempotencyConflictException();
        }
        return new TaskCreationResult(TaskResponse.from(existing), true);
    }
}
