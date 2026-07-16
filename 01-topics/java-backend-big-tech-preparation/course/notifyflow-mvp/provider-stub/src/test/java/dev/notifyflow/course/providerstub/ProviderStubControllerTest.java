package dev.notifyflow.course.providerstub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "notifyflow.provider-stub.default-commit-delay=10ms")
@AutoConfigureMockMvc
class ProviderStubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderStubService service;

    @BeforeEach
    void clearState() {
        service.clearAllForTest();
    }

    @Test
    void successCanBeDeliveredQueriedAndInspected() throws Exception {
        String body = """
                {
                  "recipientRef": "recipient-fixture-001",
                  "templateCode": "WELCOME_V1",
                  "variables": {"name": "fixture-user"}
                }
                """;

        mockMvc.perform(post("/provider/v1/deliveries")
                        .header("Idempotency-Key", "attempt-http-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(post("/provider/v1/deliveries")
                        .header("Idempotency-Key", "attempt-http-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/course/v1/effects/attempt-http-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffectCount").value(1));
    }

    @Test
    void configuredRejectReturnsDeterministicFourXxWithoutEffect() throws Exception {
        mockMvc.perform(put("/internal/course/v1/scenarios/attempt-http-reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"REJECT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("REJECT"));

        mockMvc.perform(post("/provider/v1/deliveries")
                        .header("Idempotency-Key", "attempt-http-reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientRef": "recipient-fixture-001",
                                  "templateCode": "WELCOME_V1",
                                  "variables": {}
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/internal/course/v1/effects/attempt-http-reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffectCount").value(0));
    }
}
