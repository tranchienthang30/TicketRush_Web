package ticketrush;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public final class AuthService {
    private final DataStore store;

    public AuthService(DataStore store) {
        this.store = store;
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            Map<String, Object> payload = ServiceParsers.parse(exchange);
            if ("/api/auth/register".equals(path)) {
                HttpUtil.json(exchange, 200, store.register(ServiceParsers.map(payload.get("body"))));
            } else if ("/api/auth/login".equals(path)) {
                HttpUtil.json(exchange, 200, store.login(ServiceParsers.map(payload.get("body"))));
            } else if ("/api/auth/session".equals(path)) {
                HttpUtil.json(exchange, 200, store.session(ServiceParsers.str(payload.get("token"))));
            } else {
                HttpUtil.json(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception ex) {
            HttpUtil.json(exchange, 400, Map.of("error", ex.getMessage()));
        }
    }
}
