package dev.lab.coolify.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotBlank @Size(max = 64) String type,
    @NotBlank @Size(max = 10_000) String payload
) {
}
