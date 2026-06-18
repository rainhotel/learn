package com.modelmesh.gateway.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "modelmesh")
public record ModelMeshProperties(
        Duration firstTokenTimeout,
        Map<String, ModelAlias> modelAliases,
        Map<String, ProviderChannel> channels
) {

    public record ModelAlias(
            String strategy,
            List<String> channels
    ) {
    }

    public record ProviderChannel(
            String provider,
            String baseUrl,
            String modelName,
            int weight,
            boolean enabled
    ) {
    }
}
