package com.modelmesh.gateway.core;

import com.modelmesh.gateway.config.ModelMeshProperties.ProviderChannel;

public record RoutedChannel(
        String id,
        ProviderChannel config
) {
}
