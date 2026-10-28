package dev.lab.coolify.task;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskCreationResult result = taskService.create(request, idempotencyKey);
        TaskResponse task = result.task();
        return ResponseEntity.accepted()
            .location(URI.create("/api/tasks/" + task.id()))
            .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
            .body(task);
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable UUID id) {
        return taskService.findById(id);
    }

    @GetMapping
    public Page<TaskResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return taskService.findAll(pageable);
    }
}
