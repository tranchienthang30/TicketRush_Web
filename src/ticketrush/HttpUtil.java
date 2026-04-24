package ticketrush;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class HttpUtil {
    public static final HttpClient CLIENT = HttpClient.newHttpClient();

    private HttpUtil() {}

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static Map<String, String> query(URI uri) {
        Map<String, String> map = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) return map;
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            map.put(decode(pair[0]), pair.length > 1 ? decode(pair[1]) : "");
        }
        return map;
    }

    public static void json(HttpExchange exchange, int status, Object payload) throws IOException {
        text(exchange, status, Json.stringify(payload), "application/json; charset=utf-8");
    }

    public static void text(HttpExchange exchange, int status, String payload, String contentType) throws IOException {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    public static void file(HttpExchange exchange, Path path) throws IOException {
        byte[] body = Files.readAllBytes(path);
        String type = switch (path.toString().substring(path.toString().lastIndexOf('.') + 1)) {
            case "html" -> "text/html; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "js" -> "application/javascript; charset=utf-8";
            default -> "application/octet-stream";
        };
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    public static String proxy(String method, String url, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
        if ("POST".equalsIgnoreCase(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            builder.header("Content-Type", "application/json");
        } else {
            builder.GET();
        }
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
