package dev.learn.systemdesign.v0;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class InventoryServer {
    private InventoryServer() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parseOptions(args);
        if (options.containsKey("self-test")) {
            runSelfTest();
            return;
        }

        int port = positiveOrZero(options, "port", 8080);
        int stock = positiveOrZero(options, "stock", 100_000);
        int serviceTimeMs = positiveOrZero(options, "service-time-ms", 0);
        boolean quiet = Boolean.parseBoolean(options.getOrDefault("quiet", "false"));

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "v0-request-worker");
            thread.setDaemon(false);
            return thread;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );

        InventoryState state = new InventoryState(stock);
        Metrics metrics = new Metrics();
        App app = new App(state, metrics, executor, serviceTimeMs, quiet);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 128);
        server.setExecutor(executor);
        server.createContext("/", app::handle);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdownNow();
        }, "v0-shutdown"));
        server.start();

        System.out.printf(
                "{\"event\":\"server_started\",\"time\":\"%s\",\"port\":%d,\"stock\":%d,\"serviceTimeMs\":%d}%n",
                Instant.now(), port, stock, serviceTimeMs
        );
    }

    private static void runSelfTest() {
        InventoryState state = new InventoryState(10);

        StateResult first = state.createOrder("request-a", 3);
        check(first.httpStatus == 201, "first order should be created");
        check(state.availableStock == 7, "stock should be 7");

        StateResult replay = state.createOrder("request-a", 3);
        check(replay.httpStatus == 200 && replay.replay, "same request should replay");
        check(state.availableStock == 7, "replay must not consume stock");

        StateResult conflict = state.createOrder("request-a", 4);
        check(conflict.httpStatus == 409, "same key with different payload should conflict");

        StateResult second = state.createOrder("request-b", 7);
        check(second.httpStatus == 201, "second order should consume remaining stock");
        check(state.availableStock == 0, "stock should be zero");

        StateResult rejected = state.createOrder("request-c", 1);
        check(rejected.httpStatus == 409, "insufficient stock should reject");
        check(state.checkInvariants().ok, "all invariants should hold");

        System.out.println("SELF_TEST_OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class App {
        private final InventoryState state;
        private final Metrics metrics;
        private final ThreadPoolExecutor executor;
        private final int serviceTimeMs;
        private final boolean quiet;

        private App(
                InventoryState state,
                Metrics metrics,
                ThreadPoolExecutor executor,
                int serviceTimeMs,
                boolean quiet
        ) {
            this.state = state;
            this.metrics = metrics;
            this.executor = executor;
            this.serviceTimeMs = serviceTimeMs;
            this.quiet = quiet;
        }

        private void handle(HttpExchange exchange) throws IOException {
            long started = System.nanoTime();
            Response response;
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String requestId = query.getOrDefault("requestId", "");

            try {
                response = route(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), query);
            } catch (IllegalArgumentException error) {
                response = Response.json(400, "{\"error\":\"" + jsonEscape(error.getMessage()) + "\"}");
            } catch (Exception error) {
                response = Response.json(500, "{\"error\":\"internal_error\"}");
            }

            byte[] body = response.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(response.status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();

            long durationNanos = System.nanoTime() - started;
            metrics.recordRequest(durationNanos);
            if (!quiet) {
                System.out.printf(
                        "{\"event\":\"request\",\"time\":\"%s\",\"method\":\"%s\",\"path\":\"%s\",\"status\":%d,\"durationMs\":%.3f,\"requestId\":\"%s\"}%n",
                        Instant.now(),
                        jsonEscape(exchange.getRequestMethod()),
                        jsonEscape(exchange.getRequestURI().getPath()),
                        response.status,
                        durationNanos / 1_000_000.0,
                        jsonEscape(requestId)
                );
            }
        }

        private Response route(String method, String path, Map<String, String> query) throws InterruptedException {
            if ("GET".equals(method) && "/health".equals(path)) {
                return Response.json(200, "{\"status\":\"UP\"}");
            }
            if ("GET".equals(method) && "/inventory".equals(path)) {
                return Response.json(200, state.inventoryJson());
            }
            if ("POST".equals(method) && "/orders".equals(path)) {
                if (serviceTimeMs > 0) {
                    Thread.sleep(serviceTimeMs);
                }
                String requestId = required(query, "requestId");
                int quantity = positive(query, "quantity");
                StateResult result = state.createOrder(requestId, quantity);
                metrics.recordOrder(result);
                return Response.json(result.httpStatus, result.body);
            }
            if ("GET".equals(method) && "/orders".equals(path)) {
                return state.findOrder(required(query, "requestId"));
            }
            if ("GET".equals(method) && "/invariants".equals(path)) {
                InvariantReport report = state.checkInvariants();
                return Response.json(report.ok ? 200 : 500, report.toJson());
            }
            if ("GET".equals(method) && "/metrics".equals(path)) {
                return Response.json(200, metrics.toJson(state, executor));
            }
            if ("POST".equals(method) && "/reset".equals(path)) {
                int stock = positiveOrZero(query, "stock");
                state.reset(stock);
                metrics.reset();
                return Response.json(200, state.inventoryJson());
            }
            return Response.json(404, "{\"error\":\"not_found\"}");
        }
    }

    static final class InventoryState {
        private int initialStock;
        private int availableStock;
        private long nextOrderId = 1;
        private final Map<String, OrderAttempt> attempts = new LinkedHashMap<>();

        InventoryState(int initialStock) {
            reset(initialStock);
        }

        StateResult createOrder(String requestId, int quantity) {
            OrderAttempt existing = attempts.get(requestId);
            if (existing != null) {
                if (existing.quantity != quantity) {
                    return new StateResult(
                            409,
                            false,
                            false,
                            "{\"error\":\"idempotency_key_payload_conflict\",\"requestId\":\""
                                    + jsonEscape(requestId) + "\"}"
                    );
                }
                return new StateResult(existing.httpStatus == 201 ? 200 : existing.httpStatus, true, false, existing.toJson(true));
            }

            if (quantity > availableStock) {
                OrderAttempt rejected = OrderAttempt.rejected(requestId, quantity);
                attempts.put(requestId, rejected);
                return new StateResult(409, false, true, rejected.toJson(false));
            }

            availableStock -= quantity;
            OrderAttempt created = OrderAttempt.created(nextOrderId++, requestId, quantity);
            attempts.put(requestId, created);
            return new StateResult(201, false, true, created.toJson(false));
        }

        Response findOrder(String requestId) {
            OrderAttempt attempt = attempts.get(requestId);
            if (attempt == null) {
                return Response.json(404, "{\"error\":\"order_not_found\"}");
            }
            return Response.json(200, attempt.toJson(false));
        }

        String inventoryJson() {
            return "{\"initialStock\":" + initialStock
                    + ",\"availableStock\":" + availableStock
                    + ",\"successfulOrders\":" + successfulOrderCount()
                    + ",\"attempts\":" + attempts.size() + "}";
        }

        InvariantReport checkInvariants() {
            int allocated = attempts.values().stream()
                    .filter(attempt -> "CREATED".equals(attempt.status))
                    .mapToInt(attempt -> attempt.quantity)
                    .sum();
            boolean stockNonNegative = availableStock >= 0;
            boolean conservation = initialStock == availableStock + allocated;
            boolean positiveQuantities = attempts.values().stream().allMatch(attempt -> attempt.quantity > 0);
            boolean ok = stockNonNegative && conservation && positiveQuantities;
            return new InvariantReport(ok, initialStock, availableStock, allocated, attempts.size());
        }

        int successfulOrderCount() {
            return (int) attempts.values().stream().filter(attempt -> "CREATED".equals(attempt.status)).count();
        }

        void reset(int stock) {
            if (stock < 0) {
                throw new IllegalArgumentException("stock must be >= 0");
            }
            initialStock = stock;
            availableStock = stock;
            nextOrderId = 1;
            attempts.clear();
        }
    }

    private record OrderAttempt(long orderId, String requestId, int quantity, String status, int httpStatus) {
        static OrderAttempt created(long orderId, String requestId, int quantity) {
            return new OrderAttempt(orderId, requestId, quantity, "CREATED", 201);
        }

        static OrderAttempt rejected(String requestId, int quantity) {
            return new OrderAttempt(0, requestId, quantity, "REJECTED_INSUFFICIENT_STOCK", 409);
        }

        String toJson(boolean replay) {
            return "{\"orderId\":" + orderId
                    + ",\"requestId\":\"" + jsonEscape(requestId) + "\""
                    + ",\"quantity\":" + quantity
                    + ",\"status\":\"" + status + "\""
                    + ",\"replay\":" + replay + "}";
        }
    }

    private record StateResult(int httpStatus, boolean replay, boolean countedAttempt, String body) {
    }

    private record InvariantReport(
            boolean ok,
            int initialStock,
            int availableStock,
            int allocatedStock,
            int attempts
    ) {
        String toJson() {
            return "{\"ok\":" + ok
                    + ",\"initialStock\":" + initialStock
                    + ",\"availableStock\":" + availableStock
                    + ",\"allocatedStock\":" + allocatedStock
                    + ",\"attempts\":" + attempts
                    + ",\"checks\":{\"stockNonNegative\":" + (availableStock >= 0)
                    + ",\"stockConserved\":" + (initialStock == availableStock + allocatedStock)
                    + "}}";
        }
    }

    private static final class Metrics {
        private final LongAdder requestCount = new LongAdder();
        private final LongAdder requestDurationNanos = new LongAdder();
        private final LongAdder maxRequestDurationNanos = new LongAdder();
        private final LongAdder orderCreated = new LongAdder();
        private final LongAdder orderRejected = new LongAdder();
        private final LongAdder idempotentReplay = new LongAdder();

        void recordRequest(long durationNanos) {
            requestCount.increment();
            requestDurationNanos.add(durationNanos);
            long currentMax = maxRequestDurationNanos.sum();
            if (durationNanos > currentMax) {
                maxRequestDurationNanos.reset();
                maxRequestDurationNanos.add(durationNanos);
            }
        }

        void recordOrder(StateResult result) {
            if (result.replay) {
                idempotentReplay.increment();
            } else if (result.httpStatus == 201) {
                orderCreated.increment();
            } else if (result.countedAttempt) {
                orderRejected.increment();
            }
        }

        String toJson(InventoryState state, ThreadPoolExecutor executor) {
            long requests = requestCount.sum();
            double averageMs = requests == 0 ? 0 : requestDurationNanos.sum() / 1_000_000.0 / requests;
            double maxMs = maxRequestDurationNanos.sum() / 1_000_000.0;
            return "{\"requestCount\":" + requests
                    + ",\"averageHandlerMs\":" + format(averageMs)
                    + ",\"maxHandlerMs\":" + format(maxMs)
                    + ",\"orderCreated\":" + orderCreated.sum()
                    + ",\"orderRejected\":" + orderRejected.sum()
                    + ",\"idempotentReplay\":" + idempotentReplay.sum()
                    + ",\"availableStock\":" + state.availableStock
                    + ",\"queueDepth\":" + executor.getQueue().size()
                    + ",\"activeThreads\":" + executor.getActiveCount() + "}";
        }

        void reset() {
            requestCount.reset();
            requestDurationNanos.reset();
            maxRequestDurationNanos.reset();
            orderCreated.reset();
            orderRejected.reset();
            idempotentReplay.reset();
        }
    }

    private record Response(int status, String body) {
        static Response json(int status, String body) {
            return new Response(status, body);
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new TreeMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("unknown argument: " + arg);
            }
            String option = arg.substring(2);
            int equals = option.indexOf('=');
            if (equals < 0) {
                options.put(option, "true");
            } else {
                options.put(option.substring(0, equals), option.substring(equals + 1));
            }
        }
        return options;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            query.put(key, value);
        }
        return query;
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static int positive(Map<String, String> values, String name) {
        int value = integer(required(values, name), name);
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
        return value;
    }

    private static int positiveOrZero(Map<String, String> values, String name) {
        int value = integer(required(values, name), name);
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return value;
    }

    private static int positiveOrZero(Map<String, String> options, String name, int defaultValue) {
        String raw = options.get(name);
        if (raw == null) {
            return defaultValue;
        }
        int value = integer(raw, name);
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return value;
    }

    private static int integer(String raw, String name) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}

