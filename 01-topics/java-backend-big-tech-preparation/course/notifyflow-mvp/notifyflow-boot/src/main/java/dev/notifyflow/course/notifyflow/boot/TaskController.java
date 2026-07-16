package dev.notifyflow.course.notifyflow.boot;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import dev.notifyflow.course.notifyflow.application.CreateTaskCommand;
import dev.notifyflow.course.notifyflow.application.CreateTaskResult;
import dev.notifyflow.course.notifyflow.application.CreateTaskService;
import dev.notifyflow.course.notifyflow.application.IdempotencyConflictException;
import dev.notifyflow.course.notifyflow.application.TaskCreationStatus;
import dev.notifyflow.course.notifyflow.application.TaskQueryStore;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final CreateTaskService createTaskService;
    private final TaskQueryStore taskQueryStore;

    public TaskController(
            CreateTaskService createTaskService,
            TaskQueryStore taskQueryStore) {
        this.createTaskService = createTaskService;
        this.taskQueryStore = taskQueryStore;
    }

    @PostMapping
    public ResponseEntity<TaskCreateResponse> create(
            @RequestHeader("Idempotency-Key") @NotBlank String requestId,
            @Valid @RequestBody CreateTaskHttpRequest request,
            UriComponentsBuilder uriBuilder) {
        CreateTaskResult result = createTaskService.create(new CreateTaskCommand(
                request.tenantId(),
                requestId,
                request.channel(),
                request.recipientRef(),
                request.templateCode(),
                request.variables()));

        if (result.status() == TaskCreationStatus.CONFLICT) {
            throw new IdempotencyConflictException(request.tenantId(), requestId);
        }

        NotificationTask task = result.task();
        TaskCreateResponse response = new TaskCreateResponse(
                task.id(),
                task.status().name(),
                task.version(),
                result.status() == TaskCreationStatus.REPLAYED);
        URI location = uriBuilder.path("/api/v1/tasks/{id}").buildAndExpand(task.id()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).body(response);
    }

    @GetMapping("/{taskId}")
    public TaskView get(@PathVariable long taskId) {
        if (taskId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId must be positive");
        }
        return taskQueryStore.findById(taskId)
                .map(TaskView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ErrorResponse> conflict(IdempotencyConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("IDEMPOTENCY_CONFLICT", exception.getMessage()));
    }

    public record CreateTaskHttpRequest(
            @NotBlank String tenantId,
            @NotBlank String channel,
            @NotBlank String recipientRef,
            @NotBlank String templateCode,
            @NotNull Map<String, String> variables) {
    }

    public record TaskCreateResponse(long taskId, String status, long version, boolean replayed) {
    }

    public record TaskView(
            long taskId,
            String tenantId,
            String status,
            int currentAttemptNo,
            String lastErrorCategory,
            String lastErrorCode,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static TaskView from(NotificationTask task) {
            return new TaskView(task.id(), task.tenantId(), task.status().name(), task.currentAttemptNo(),
                    task.lastErrorCategory(), task.lastErrorCode(), task.version(),
                    task.createdAt(), task.updatedAt());
        }
    }

    public record ErrorResponse(String code, String message) {
    }

}
