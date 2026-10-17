import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Loopback-only renderer test server. Synthetic snapshots exist only at explicit /cases/ URLs.
 * Run with JDK 17 source-file mode; no dependencies or generated classes enter the repository.
 */
public final class DashboardTestServer {
    private static final Map<String, String> FIXTURES = Map.of(
            "full", "full.json", "null", "null.json", "schema", "schema.json", "invalid", "invalid.txt");

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java DashboardTestServer.java <dashboard-root> <port>");
        }
        Path root = Path.of(args[0]).toRealPath();
        int port = Integer.parseInt(args[1]);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        var executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", exchange -> serve(exchange, root));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdownNow();
        }));
        server.start();
        System.out.println("DASHBOARD-TEST-SERVER http://127.0.0.1:" + port + "/tests/index.html");
        System.out.println("Synthetic fixtures are test data, not gameplay or acceptance evidence.");
    }

    private static void serve(HttpExchange exchange, Path root) throws IOException {
        try (exchange) {
            if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
                reply(exchange, 405, "text/plain", "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }
            String requested = exchange.getRequestURI().getPath();
            String relative = requested.substring(1);
            if (requested.startsWith("/cases/")) {
                String[] parts = requested.split("/", 4);
                if (parts.length != 4 || (!FIXTURES.containsKey(parts[2]) && !parts[2].equals("missing"))) {
                    reply(exchange, 404, "text/plain", new byte[0]);
                    return;
                }
                if (parts[3].equals("data.json")) {
                    String fixture = FIXTURES.get(parts[2]);
                    if (fixture == null) {
                        reply(exchange, 404, "application/json", new byte[0]);
                        return;
                    }
                    relative = "tests/fixtures/" + fixture;
                } else {
                    relative = parts[3];
                }
            }
            if (relative.isEmpty() || relative.endsWith("/")) relative += "index.html";
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file) || !file.toRealPath().startsWith(root)) {
                reply(exchange, 404, "text/plain", new byte[0]);
                return;
            }
            String type = file.toString().endsWith(".css") ? "text/css"
                    : file.toString().endsWith(".js") ? "text/javascript"
                    : file.toString().endsWith(".json") ? "application/json"
                    : file.toString().endsWith(".html") ? "text/html" : "text/plain";
            reply(exchange, 200, type, Files.readAllBytes(file));
        }
    }

    private static void reply(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
