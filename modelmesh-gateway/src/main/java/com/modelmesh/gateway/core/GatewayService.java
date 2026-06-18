package com.modelmesh.gateway.core;

import com.modelmesh.gateway.dto.ChatCompletionRequest;
import com.modelmesh.gateway.dto.ChatCompletionResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class GatewayService {
    private final ChannelSelector channelSelector;
    private final ProviderClient providerClient;

    public GatewayService(ChannelSelector channelSelector, ProviderClient providerClient) {
        this.channelSelector = channelSelector;
        this.providerClient = providerClient;
    }

    public Mono<ChatCompletionResponse> complete(ChatCompletionRequest request) {
        List<RoutedChannel> channels = channelSelector.orderedChannels(request.model());
        return completeWithFallback(channels, request, 0);
    }

    public Flux<ServerSentEvent<String>> stream(ChatCompletionRequest request) {
        List<RoutedChannel> channels = channelSelector.orderedChannels(request.model());
        return streamWithFallback(channels, request, 0);
    }

    private Mono<ChatCompletionResponse> completeWithFallback(
            List<RoutedChannel> channels,
            ChatCompletionRequest request,
            int index
    ) {
        if (index >= channels.size()) {
            return Mono.error(new GatewayException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "All upstream channels failed"
            ));
        }

        return providerClient.complete(channels.get(index), request)
                .onErrorResume(ignored -> completeWithFallback(channels, request, index + 1));
    }

    private Flux<ServerSentEvent<String>> streamWithFallback(
            List<RoutedChannel> channels,
            ChatCompletionRequest request,
            int index
    ) {
        if (index >= channels.size()) {
            return Flux.error(new GatewayException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "All upstream channels failed before streaming started"
            ));
        }

        AtomicBoolean emitted = new AtomicBoolean(false);
        return providerClient.stream(channels.get(index), request)
                .doOnNext(ignored -> emitted.set(true))
                .map(data -> ServerSentEvent.builder(data).build())
                .onErrorResume(error -> {
                    if (emitted.get()) {
                        return Flux.error(error);
                    }
                    return streamWithFallback(channels, request, index + 1);
                });
    }
}
