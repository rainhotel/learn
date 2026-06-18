package com.modelmesh.gateway.dto;

import java.util.List;

public record ChatCompletionResponse(
        String id,
        String object,
        long created,
        String model,
        List<ChatChoice> choices,
        Usage usage
) {
}
