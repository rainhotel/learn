package com.modelmesh.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatChoice(
        int index,
        ChatMessage message,
        @JsonProperty("finish_reason") String finishReason
) {
}
