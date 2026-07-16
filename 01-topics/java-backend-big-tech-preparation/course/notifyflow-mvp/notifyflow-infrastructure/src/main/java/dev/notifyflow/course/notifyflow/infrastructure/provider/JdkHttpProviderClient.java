package dev.notifyflow.course.notifyflow.infrastructure.provider;

import dev.notifyflow.course.notifyflow.application.ProviderDeliveryCommand;
import dev.notifyflow.course.notifyflow.application.ProviderGateway;
import dev.notifyflow.course.notifyflow.application.ProviderQueryCommand;
import dev.notifyflow.course.notifyflow.application.ProviderQueryGateway;
import dev.notifyflow.course.notifyflow.application.ProviderCallResult;
import dev.notifyflow.course.notifyflow.application.ProviderQueryResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Small JDK-only adapter for the course provider stub. HTTP is deliberately
 * outside the database transaction and every ambiguous transport outcome is
 * represented as UNKNOWN/PENDING rather than guessed as a failure.
 */
public final class JdkHttpProviderClient implements ProviderGateway, ProviderQueryGateway {
    private final HttpClient client;
    private final URI baseUri;
    private final Duration requestTimeout;
    private final Clock clock;

    public JdkHttpProviderClient(URI baseUri, Duration connectTimeout, Duration requestTimeout) {
        this(baseUri, connectTimeout, requestTimeout, Clock.systemUTC());
    }

    public JdkHttpProviderClient(URI baseUri, Duration connectTimeout, Duration requestTimeout, Clock clock) {
        this.baseUri = normalizeBase(Objects.requireNonNull(baseUri, "baseUri"));
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.requestTimeout = requestTimeout;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public ProviderCallResult deliver(ProviderDeliveryCommand command) {
        Objects.requireNonNull(command, "command");
        URI uri = baseUri.resolve("provider/v1/deliveries");
        String body = "{\"recipientRef\":" + quote(command.recipientRef())
                + ",\"templateCode\":" + quote(command.templateCode())
                + ",\"variables\":" + objectJson(command.variablesJson()) + "}";
        HttpRequest request;
        try {
            request = request(uri, command.idempotencyKey(), body, command.deadlineAt());
        } catch (DeadlineExceeded exception) {
            return ProviderCallResult.unknown(null, "TIMEOUT", "DEADLINE_EXCEEDED");
        }
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String providerRequestId = field(response.body(), "providerRequestId");
            String status = field(response.body(), "status");
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return classifyDelivery(providerRequestId, status, field(response.body(), "errorCode"));
            }
            if (response.statusCode() >= 400 && response.statusCode() < 500) {
                return ProviderCallResult.rejected(providerRequestId, "PERMANENT",
                        firstNonBlank(field(response.body(), "errorCode"), "HTTP_" + response.statusCode()));
            }
            return ProviderCallResult.unknown(providerRequestId, "TRANSIENT", "HTTP_" + response.statusCode());
        } catch (HttpTimeoutException exception) {
            return ProviderCallResult.unknown(null, "TIMEOUT", "PROVIDER_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ProviderCallResult.unknown(null, "INTERRUPTED", "PROVIDER_CALL_INTERRUPTED");
        } catch (IOException | RuntimeException exception) {
            return ProviderCallResult.unknown(null, "IO", "PROVIDER_IO_ERROR");
        }
    }

    @Override
    public ProviderQueryResult query(ProviderQueryCommand command) {
        Objects.requireNonNull(command, "command");
        URI uri = baseUri.resolve("provider/v1/deliveries/by-idempotency-key/" + encodePath(command.idempotencyKey()));
        HttpRequest request;
        try {
            request = request(uri, null, null, command.deadlineAt());
        } catch (DeadlineExceeded exception) {
            return new ProviderQueryResult(ProviderQueryResult.Status.PENDING, command.providerRequestId(),
                    "TIMEOUT", "RECONCILIATION_DEADLINE_EXCEEDED");
        }
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String providerRequestId = firstNonBlank(field(response.body(), "providerRequestId"), command.providerRequestId());
            String status = field(response.body(), "status");
            if (response.statusCode() == 404) {
                return new ProviderQueryResult(ProviderQueryResult.Status.NOT_FOUND, providerRequestId,
                        "NOT_FOUND", "PROVIDER_NOT_FOUND");
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return classifyQuery(providerRequestId, status, field(response.body(), "errorCode"));
            }
            return new ProviderQueryResult(ProviderQueryResult.Status.PENDING, providerRequestId,
                    response.statusCode() >= 500 ? "TRANSIENT" : "PROTOCOL", "HTTP_" + response.statusCode());
        } catch (HttpTimeoutException exception) {
            return new ProviderQueryResult(ProviderQueryResult.Status.PENDING, command.providerRequestId(),
                    "TIMEOUT", "PROVIDER_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ProviderQueryResult(ProviderQueryResult.Status.PENDING, command.providerRequestId(),
                    "INTERRUPTED", "PROVIDER_QUERY_INTERRUPTED");
        } catch (IOException | RuntimeException exception) {
            return new ProviderQueryResult(ProviderQueryResult.Status.PENDING, command.providerRequestId(),
                    "IO", "PROVIDER_IO_ERROR");
        }
    }

    private ProviderCallResult classifyDelivery(String providerRequestId, String status, String errorCode) {
        if ("SUCCEEDED".equals(status)) {
            return ProviderCallResult.success(providerRequestId);
        }
        if ("REJECTED".equals(status)) {
            return ProviderCallResult.rejected(providerRequestId, "PERMANENT",
                    firstNonBlank(errorCode, "PROVIDER_REJECTED"));
        }
        return ProviderCallResult.unknown(providerRequestId, "PROTOCOL", "UNSUPPORTED_PROVIDER_STATUS");
    }

    private ProviderQueryResult classifyQuery(String providerRequestId, String status, String errorCode) {
        ProviderQueryResult.Status mapped;
        try {
            mapped = ProviderQueryResult.Status.valueOf(status == null ? "" : status);
        } catch (IllegalArgumentException exception) {
            mapped = ProviderQueryResult.Status.PENDING;
        }
        String category = mapped == ProviderQueryResult.Status.REJECTED ? "PERMANENT" : null;
        String code = mapped == ProviderQueryResult.Status.REJECTED
                ? firstNonBlank(errorCode, "PROVIDER_REJECTED") : errorCode;
        return new ProviderQueryResult(mapped, providerRequestId, category, code);
    }

    private HttpRequest request(URI uri, String idempotencyKey, String body, Instant deadline) {
        Duration timeout = requestTimeout;
        if (deadline != null) {
            Duration remaining = Duration.between(clock.instant(), deadline);
            if (remaining.isNegative() || remaining.isZero()) {
                throw new DeadlineExceeded();
            }
            timeout = remaining.compareTo(timeout) < 0 ? remaining : timeout;
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json");
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        if (body == null) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return builder.build();
    }

    private static URI normalizeBase(URI base) {
        String value = base.toString();
        return URI.create(value.endsWith("/") ? value : value + "/");
    }

    private static String objectJson(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        String trimmed = value.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}") ? trimmed : "{}";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> result.append(c);
            }
        }
        return result.append('"').toString();
    }

    private static String field(String json, String name) {
        if (json == null) return null;
        String needle = quote(name) + ":";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        int value = start + needle.length();
        while (value < json.length() && Character.isWhitespace(json.charAt(value))) value++;
        if (value >= json.length() || json.startsWith("null", value)) return null;
        if (json.charAt(value) != '"') return null;
        StringBuilder result = new StringBuilder();
        for (int i = value + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return result.toString();
            if (c == '\\' && i + 1 < json.length()) {
                char escaped = json.charAt(++i);
                result.append(switch (escaped) {
                    case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; case '"' -> '"'; case '\\' -> '\\'; default -> escaped;
                });
            } else result.append(c);
        }
        return null;
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String encodePath(String value) {
        return value.replace("%", "%25").replace("/", "%2F").replace("?", "%3F").replace("#", "%23");
    }

    private static final class DeadlineExceeded extends RuntimeException {
    }
}
