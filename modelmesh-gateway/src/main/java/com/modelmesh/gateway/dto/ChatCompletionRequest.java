package com.modelmesh.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ChatCompletionRequest(
        @NotBlank String model,
        @NotEmpty List<@Valid ChatMessage> messages,
        Boolean stream,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens
) {
    public boolean streaming() {
        return Boolean.TRUE.equals(stream);
    }

    public ChatCompletionRequest withModel(String model) {
        return new ChatCompletionRequest(model, messages, stream, temperature, maxTokens);
    }
}
