package dev.lab.coolify.web;

import dev.lab.coolify.task.IdempotencyConflictException;
import dev.lab.coolify.task.InvalidIdempotencyKeyException;
import dev.lab.coolify.task.TaskNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleNotFound(TaskNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Task not found");
        problem.setType(URI.create("https://example.invalid/problems/task-not-found"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Request validation failed");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid request");
        problem.setType(URI.create("https://example.invalid/problems/validation"));
        return problem;
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    ProblemDetail handleInvalidIdempotencyKey(InvalidIdempotencyKeyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid Idempotency-Key");
        problem.setType(URI.create("https://example.invalid/problems/idempotency-key"));
        return problem;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Idempotency conflict");
        problem.setType(URI.create("https://example.invalid/problems/idempotency-conflict"));
        return problem;
    }
}
