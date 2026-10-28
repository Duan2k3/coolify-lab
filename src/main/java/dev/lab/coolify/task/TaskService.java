package dev.lab.coolify.task;

import java.util.UUID;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCreationTransaction taskCreationTransaction;
    private final TaskCache taskCache;
    private final Counter createdCounter;
    private final Counter replayedCounter;

    public TaskService(
        TaskRepository taskRepository,
        TaskCreationTransaction taskCreationTransaction,
        TaskCache taskCache,
        MeterRegistry meterRegistry
    ) {
        this.taskRepository = taskRepository;
        this.taskCreationTransaction = taskCreationTransaction;
        this.taskCache = taskCache;
        this.createdCounter = meterRegistry.counter("tasks.created.total");
        this.replayedCounter = meterRegistry.counter("tasks.idempotency.replayed.total");
    }

    public TaskCreationResult create(CreateTaskRequest request, String rawIdempotencyKey) {
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        TaskCreationResult result;
        try {
            result = taskCreationTransaction.create(request, idempotencyKey);
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyKey == null) {
                throw exception;
            }
            result = taskCreationTransaction.findReplay(idempotencyKey, request);
        }

        if (result.replayed()) {
            replayedCounter.increment();
        } else {
            createdCounter.increment();
        }
        taskCache.put(result.task());
        return result;
    }

    public TaskResponse findById(UUID id) {
        return taskCache.get(id).orElseGet(() -> {
            TaskResponse task = findByIdFromDatabase(id);
            taskCache.put(task);
            return task;
        });
    }

    @Transactional(readOnly = true)
    TaskResponse findByIdFromDatabase(UUID id) {
        return taskRepository.findById(id)
            .map(TaskResponse::from)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(Pageable pageable) {
        return taskRepository.findAll(pageable).map(TaskResponse::from);
    }

    private String normalizeIdempotencyKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        String key = rawKey.trim();
        if (key.isEmpty()) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key must not be blank");
        }
        if (key.length() > 128) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key must not exceed 128 characters");
        }
        return key;
    }
}
