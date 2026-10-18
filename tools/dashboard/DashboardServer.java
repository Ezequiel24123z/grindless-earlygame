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

/** Production-only variant of DashboardTestServer: exact artifact allowlist, never synthetic cases. */
public final class DashboardServer {
    private static final Map<String, String> FILES = Map.of(
            "index.html", "text/html", "dashboard.css", "text/css",
            "dashboard.js", "text/javascript", "dashboard-model.js", "text/javascript",
            "data.json", "application/json", "provenance.json", "application/json",
            "audit.json", "application/json", "audit.md", "text/plain");

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java DashboardServer.java <artifact-directory> <port>");
        }
        Path root = Path.of(args[0]).toRealPath();
        for (String name : FILES.keySet()) {
            Path file = root.resolve(name);
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file) || !file.toRealPath().startsWith(root)) {
                throw new IOException("A complete, non-symlink dashboard artifact is required.");
            }
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),
                Integer.parseInt(args[1])), 0);
        var executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.createContext("/", exchange -> serve(exchange, root));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdownNow();
        }));
        server.start();
        System.out.println("DASHBOARD http://127.0.0.1:" + server.getAddress().getPort() + "/index.html");
        System.out.println("Canonical generated snapshot only; refresh does not rerun an audit or acceptance check.");
    }

    private static void serve(HttpExchange exchange, Path root) throws IOException {
        try (exchange) {
            if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
                reply(exchange, 405, "text/plain", new byte[0]);
                return;
            }
            String requested = exchange.getRequestURI().getPath();
            String name = requested.equals("/") ? "index.html" : requested.substring(1);
            if (!FILES.containsKey(name)) {
                reply(exchange, 404, "text/plain", new byte[0]);
                return;
            }
            Path file = root.resolve(name);
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file) || !file.toRealPath().startsWith(root)) {
                reply(exchange, 404, "text/plain", new byte[0]);
                return;
            }
            reply(exchange, 200, FILES.get(name), Files.readAllBytes(file));
        }
    }

    private static void reply(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self'; "
                        + "img-src 'self'; font-src 'none'; object-src 'none'; base-uri 'none'; "
                        + "form-action 'none'; frame-ancestors 'self'");
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
