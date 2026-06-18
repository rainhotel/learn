package com.modelmesh.gateway.api;

import com.modelmesh.gateway.dto.ChatChoice;
import com.modelmesh.gateway.dto.ChatCompletionRequest;
import com.modelmesh.gateway.dto.ChatCompletionResponse;
import com.modelmesh.gateway.dto.ChatMessage;
import com.modelmesh.gateway.dto.Usage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/mock")
public class MockProviderController {

    @PostMapping("/v1/chat/completions")
    public Mono<ResponseEntity<?>> createMockCompletion(
            @RequestBody ChatCompletionRequest request,
            @RequestHeader(name = "x-mock-mode", defaultValue = "ok") String mode
    ) {
        if ("429".equals(mode)) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "mock rate limit")));
        }
        if ("500".equals(mode)) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "mock upstream failure")));
        }
        if (request.streaming()) {
            ResponseEntity<Flux<ServerSentEvent<String>>> response = ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(mockStream(request, mode));
            return Mono.just(response);
        }

        Mono<ChatCompletionResponse> response = Mono.just(mockResponse(request));
        if ("slow".equals(mode)) {
            response = response.delayElement(Duration.ofSeconds(3));
        }
        return response.map(ResponseEntity::ok);
    }

    private Flux<ServerSentEvent<String>> mockStream(ChatCompletionRequest request, String mode) {
        Flux<ServerSentEvent<String>> stream = Flux.just(
                        chunk(request.model(), "Hello"),
                        chunk(request.model(), " from"),
                        chunk(request.model(), " ModelMesh"),
                        ServerSentEvent.<String>builder("[DONE]").build()
                )
                .delayElements("slow".equals(mode) ? Duration.ofSeconds(3) : Duration.ofMillis(120));

        if ("stream-drop".equals(mode)) {
            return stream.take(2).concatWith(Flux.error(new IllegalStateException("mock stream dropped")));
        }
        return stream;
    }

    private ServerSentEvent<String> chunk(String model, String content) {
        String json = """
                {"id":"%s","object":"chat.completion.chunk","created":%d,"model":"%s","choices":[{"index":0,"delta":{"content":"%s"},"finish_reason":null}]}
                """.formatted("chatcmpl-" + UUID.randomUUID(), Instant.now().getEpochSecond(), model, content.trim());
        return ServerSentEvent.builder(json).build();
    }

    private ChatCompletionResponse mockResponse(ChatCompletionRequest request) {
        String text = "Hello from ModelMesh mock provider.";
        int promptTokens = request.messages().stream()
                .mapToInt(message -> message.content().split("\\s+").length)
                .sum();
        int completionTokens = text.split("\\s+").length;
        return new ChatCompletionResponse(
                "chatcmpl-" + UUID.randomUUID(),
                "chat.completion",
                Instant.now().getEpochSecond(),
                request.model(),
                List.of(new ChatChoice(0, new ChatMessage("assistant", text), "stop")),
                new Usage(promptTokens, completionTokens, promptTokens + completionTokens)
        );
    }
}
