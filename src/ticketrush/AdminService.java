package ticketrush;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public final class AdminService {
    private final DataStore store;

    public AdminService(DataStore store) {
        this.store = store;
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("/api/admin/dashboard".equals(path)) {
                HttpUtil.json(exchange, 200, store.dashboard());
            } else if ("/api/admin/shows".equals(path)) {
                HttpUtil.json(exchange, 200, store.createShow(ServiceParsers.map(ServiceParsers.parse(exchange).get("body"))));
            } else {
                HttpUtil.json(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception ex) {
            HttpUtil.json(exchange, 400, Map.of("error", ex.getMessage()));
        }
    }
}
