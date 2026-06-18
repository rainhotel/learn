package com.modelmesh.gateway.core;

import com.modelmesh.gateway.config.ModelMeshProperties;
import com.modelmesh.gateway.dto.ChatCompletionRequest;
import com.modelmesh.gateway.dto.ChatCompletionResponse;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProviderClient {
    private final WebClient.Builder webClientBuilder;
    private final Duration firstTokenTimeout;

    public ProviderClient(WebClient.Builder webClientBuilder, ModelMeshProperties properties) {
        this.webClientBuilder = webClientBuilder;
        this.firstTokenTimeout = properties.firstTokenTimeout();
    }

    public Mono<ChatCompletionResponse> complete(RoutedChannel channel, ChatCompletionRequest request) {
        ChatCompletionRequest upstreamRequest = request.withModel(channel.config().modelName());
        return webClient(channel)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(upstreamRequest)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(firstTokenTimeout);
    }

    public Flux<String> stream(RoutedChannel channel, ChatCompletionRequest request) {
        ChatCompletionRequest upstreamRequest = request.withModel(channel.config().modelName());
        return webClient(channel)
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(upstreamRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(firstTokenTimeout);
    }

    private WebClient webClient(RoutedChannel channel) {
        return webClientBuilder.baseUrl(channel.config().baseUrl()).build();
    }
}
