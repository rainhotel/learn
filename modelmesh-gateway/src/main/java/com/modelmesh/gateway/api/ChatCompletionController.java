package com.modelmesh.gateway.api;

import com.modelmesh.gateway.core.GatewayException;
import com.modelmesh.gateway.core.GatewayService;
import com.modelmesh.gateway.dto.ChatCompletionRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ChatCompletionController {
    private final GatewayService gatewayService;

    public ChatCompletionController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping("/v1/chat/completions")
    public Mono<ResponseEntity<?>> createChatCompletion(@Valid @RequestBody ChatCompletionRequest request) {
        if (request.streaming()) {
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(gatewayService.stream(request)));
        }

        return gatewayService.complete(request)
                .map(ResponseEntity::ok);
    }

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<Map<String, Object>> handleGatewayException(GatewayException exception) {
        return ResponseEntity.status(exception.status())
                .body(Map.of("error", exception.getMessage()));
    }
}
