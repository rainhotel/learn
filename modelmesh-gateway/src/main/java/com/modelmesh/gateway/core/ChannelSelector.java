package com.modelmesh.gateway.core;

import com.modelmesh.gateway.config.ModelMeshProperties;
import com.modelmesh.gateway.config.ModelMeshProperties.ModelAlias;
import com.modelmesh.gateway.config.ModelMeshProperties.ProviderChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ChannelSelector {
    private final ModelMeshProperties properties;
    private final Map<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

    public ChannelSelector(ModelMeshProperties properties) {
        this.properties = properties;
    }

    public List<RoutedChannel> orderedChannels(String modelAlias) {
        ModelAlias alias = properties.modelAliases().get(modelAlias);
        if (alias == null) {
            throw new GatewayException(HttpStatus.NOT_FOUND, "Unknown model alias: " + modelAlias);
        }

        List<RoutedChannel> weighted = new ArrayList<>();
        for (String channelId : alias.channels()) {
            ProviderChannel channel = properties.channels().get(channelId);
            if (channel == null || !channel.enabled()) {
                continue;
            }
            int weight = Math.max(1, channel.weight());
            for (int i = 0; i < weight; i++) {
                weighted.add(new RoutedChannel(channelId, channel));
            }
        }

        if (weighted.isEmpty()) {
            throw new GatewayException(HttpStatus.SERVICE_UNAVAILABLE, "No enabled channels for model alias: " + modelAlias);
        }

        int cursor = cursors.computeIfAbsent(modelAlias, ignored -> new AtomicInteger()).getAndIncrement();
        int start = Math.floorMod(cursor, weighted.size());
        List<RoutedChannel> ordered = new ArrayList<>();
        for (int i = 0; i < weighted.size(); i++) {
            ordered.add(weighted.get((start + i) % weighted.size()));
        }

        return ordered.stream()
                .distinct()
                .sorted(Comparator.comparingInt(channel -> ordered.indexOf(channel)))
                .toList();
    }
}
