package dev.notifyflow.course.notifyflow.boot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import dev.notifyflow.course.notifyflow.application.CreateTaskResult;
import dev.notifyflow.course.notifyflow.application.CreateTaskService;
import dev.notifyflow.course.notifyflow.application.IdempotencyConflictException;
import dev.notifyflow.course.notifyflow.application.TaskCreationStatus;
import dev.notifyflow.course.notifyflow.application.TaskQueryStore;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-15T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateTaskService createTaskService;

    @MockBean
    private TaskQueryStore taskQueryStore;

    @Test
    void createReturnsAcceptedAndLocation() throws Exception {
        NotificationTask task = task(10001L, TaskStatus.ACCEPTED, 0L);
        when(createTaskService.create(any())).thenReturn(
                new CreateTaskResult(TaskCreationStatus.CREATED, task, "a".repeat(64)));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Idempotency-Key", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-course",
                                  "channel": "COURSE_STUB",
                                  "recipientRef": "recipient-fixture-001",
                                  "templateCode": "WELCOME_V1",
                                  "variables": {"name": "fixture-user"}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/tasks/10001"))
                .andExpect(jsonPath("$.taskId").value(10001))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.replayed").value(false));
    }

    @Test
    void replayReturnsAcceptedAndMarksReplay() throws Exception {
        NotificationTask task = task(10001L, TaskStatus.ACCEPTED, 0L);
        when(createTaskService.create(any())).thenReturn(
                new CreateTaskResult(TaskCreationStatus.REPLAYED, task, "a".repeat(64)));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Idempotency-Key", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-course",
                                  "channel": "COURSE_STUB",
                                  "recipientRef": "recipient-fixture-001",
                                  "templateCode": "WELCOME_V1",
                                  "variables": {"name": "fixture-user"}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.replayed").value(true));
    }

    @Test
    void conflictReturns409() throws Exception {
        when(createTaskService.create(any()))
                .thenThrow(new IdempotencyConflictException("tenant-course", "req-1"));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Idempotency-Key", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-course",
                                  "channel": "COURSE_STUB",
                                  "recipientRef": "recipient-fixture-001",
                                  "templateCode": "WELCOME_V1",
                                  "variables": {"name": "different"}
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void getHidesSensitiveDeliveryFields() throws Exception {
        when(taskQueryStore.findById(10001L)).thenReturn(
                Optional.of(task(10001L, TaskStatus.UNKNOWN, 2L)));

        mockMvc.perform(get("/api/v1/tasks/10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(10001))
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.recipientRef").doesNotExist())
                .andExpect(jsonPath("$.variablesJson").doesNotExist());
    }

    private static NotificationTask task(long id, TaskStatus status, long version) {
        return new NotificationTask(id, "tenant-course", "req-1", "a".repeat(64),
                "COURSE_STUB", "recipient-fixture-001", "WELCOME_V1", "{\"name\":\"fixture-user\"}",
                status, status == TaskStatus.UNKNOWN ? 1 : 0, "UNKNOWN", "PROVIDER_TIMEOUT",
                version, CREATED_AT, CREATED_AT.plusSeconds(1));
    }
}
