package ticketrush;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class GatewayService {
    private final Path staticDir;

    public GatewayService(Path staticDir) {
        this.staticDir = staticDir;
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (!path.startsWith("/api/")) {
                Path target = resolveStaticTarget(path);
                HttpUtil.file(exchange, target);
                return;
            }
            proxy(exchange, path);
        } catch (Exception ex) {
            HttpUtil.json(exchange, 500, Map.of("error", ex.getMessage()));
        }
    }

    private Path resolveStaticTarget(String path) {
        if ("/".equals(path)) return staticDir.resolve("html").resolve("home.html");

        String relative = path.startsWith("/") ? path.substring(1) : path;
        Path target = staticDir.resolve(relative);
        if (target.toFile().exists()) return target;

        if (relative.endsWith(".html")) {
            Path htmlTarget = staticDir.resolve("html").resolve(relative);
            if (htmlTarget.toFile().exists()) return htmlTarget;
        }

        return staticDir.resolve("html").resolve("home.html");
    }

    private void proxy(HttpExchange exchange, String path) throws IOException, InterruptedException {
        String body = "POST".equalsIgnoreCase(exchange.getRequestMethod()) ? HttpUtil.readBody(exchange) : "";
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        String clientId = exchange.getRequestHeaders().getFirst("X-Client-Id");
        Map<String, Object> envelope = Map.of(
            "token", token == null ? "" : token.replace("Bearer ", ""),
            "clientIdHeader", clientId == null ? "" : clientId,
            "body", body.isBlank() ? Map.of() : Json.parse(body)
        );
        String base = switch (path) {
            case "/api/catalog", "/api/pages/home" -> "http://127.0.0.1:9002";
            case "/api/auth/login", "/api/auth/register", "/api/auth/session" -> "http://127.0.0.1:9001";
            case "/api/admin/dashboard", "/api/admin/shows" -> "http://127.0.0.1:9004";
            default -> path.startsWith("/api/shows/") ? "http://127.0.0.1:9003" : "http://127.0.0.1:9002";
        };
        String url = base + path;
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null && !query.isBlank()) url += "?" + query;
        String response = HttpUtil.proxy(exchange.getRequestMethod(), url, Json.stringify(envelope));
        HttpUtil.text(exchange, 200, response, "application/json; charset=utf-8");
    }
}
