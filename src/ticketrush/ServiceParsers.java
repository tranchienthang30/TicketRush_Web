package ticketrush;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class ServiceParsers {
    private ServiceParsers() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(HttpExchange exchange) throws IOException {
        String body = HttpUtil.readBody(exchange);
        return body == null || body.isBlank() ? Map.of() : (Map<String, Object>) Json.parse(body);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object value) {
        return value == null ? Map.of() : (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object value) {
        return value == null ? List.of() : (List<Object>) value;
    }

    public static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
