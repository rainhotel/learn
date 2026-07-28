package dev.learn.systemdesign.v0;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class LoadGenerator {
    private LoadGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parseOptions(args);
        String baseUrl = options.getOrDefault("base-url", "http://127.0.0.1:8080");
        int requests = positive(options, "requests", 1_000);
        int concurrency = positive(options, "concurrency", 8);
        int quantity = positive(options, "quantity", 1);
        int resetStock = positive(options, "reset-stock", requests * quantity + 100);
        String output = options.get("output");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        post(client, baseUrl + "/reset?stock=" + resetStock);

        long[] latenciesNanos = new long[requests];
        int[] statuses = new int[requests];
        AtomicInteger next = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(concurrency);
        ExecutorService workers = Executors.newFixedThreadPool(concurrency);
        String runId = UUID.randomUUID().toString();

        for (int worker = 0; worker < concurrency; worker++) {
            workers.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    while (true) {
                        int index = next.getAndIncrement();
                        if (index >= requests) {
                            break;
                        }
                        String requestId = runId + "-" + index;
                        String url = baseUrl + "/orders?requestId="
                                + URLEncoder.encode(requestId, StandardCharsets.UTF_8)
                                + "&quantity=" + quantity;
                        long before = System.nanoTime();
                        try {
                            HttpResponse<String> response = post(client, url);
                            statuses[index] = response.statusCode();
                        } catch (Exception error) {
                            statuses[index] = 0;
                        }
                        latenciesNanos[index] = System.nanoTime() - before;
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        long experimentStart = System.nanoTime();
        start.countDown();
        done.await();
        long elapsedNanos = System.nanoTime() - experimentStart;
        workers.shutdown();
        workers.awaitTermination(5, TimeUnit.SECONDS);

        long[] sorted = Arrays.copyOf(latenciesNanos, latenciesNanos.length);
        Arrays.sort(sorted);
        Map<Integer, Integer> statusCounts = new TreeMap<>();
        for (int status : statuses) {
            statusCounts.merge(status, 1, Integer::sum);
        }

        String invariants = get(client, baseUrl + "/invariants").body();
        String serverMetrics = get(client, baseUrl + "/metrics").body();
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double throughput = requests / elapsedSeconds;
        String result = "{\n"
                + "  \"time\": \"" + Instant.now() + "\",\n"
                + "  \"requests\": " + requests + ",\n"
                + "  \"concurrency\": " + concurrency + ",\n"
                + "  \"quantity\": " + quantity + ",\n"
                + "  \"elapsedSeconds\": " + format(elapsedSeconds) + ",\n"
                + "  \"throughputRps\": " + format(throughput) + ",\n"
                + "  \"latencyMs\": {\"p50\": " + format(toMs(percentile(sorted, 0.50)))
                + ", \"p95\": " + format(toMs(percentile(sorted, 0.95)))
                + ", \"p99\": " + format(toMs(percentile(sorted, 0.99)))
                + ", \"max\": " + format(toMs(sorted[sorted.length - 1])) + "},\n"
                + "  \"statusCounts\": " + mapToJson(statusCounts) + ",\n"
                + "  \"invariants\": " + invariants + ",\n"
                + "  \"serverMetrics\": " + serverMetrics + "\n"
                + "}";

        System.out.println(result);
        if (output != null) {
            Path outputPath = Path.of(output).toAbsolutePath().normalize();
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, result + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    private static HttpResponse<String> post(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static long percentile(long[] sorted, double percentile) {
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index];
    }

    private static double toMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static String mapToJson(Map<Integer, Integer> values) {
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            if (!first) {
                result.append(", ");
            }
            first = false;
            result.append('\"').append(entry.getKey()).append("\": ").append(entry.getValue());
        }
        return result.append('}').toString();
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new TreeMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException("expected --name=value, got: " + arg);
            }
            int equals = arg.indexOf('=');
            options.put(arg.substring(2, equals), arg.substring(equals + 1));
        }
        return options;
    }

    private static int positive(Map<String, String> options, String name, int defaultValue) {
        String raw = options.get(name);
        if (raw == null) {
            return defaultValue;
        }
        int value = Integer.parseInt(raw);
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
        return value;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}

