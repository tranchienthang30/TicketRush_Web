package ticketrush;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public final class CatalogService {
    private final DataStore store;

    public CatalogService(DataStore store) {
        this.store = store;
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("/api/catalog".equals(path) || "/api/pages/home".equals(path)) {
                HttpUtil.json(exchange, 200, store.catalog());
            } else {
                HttpUtil.json(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception ex) {
            HttpUtil.json(exchange, 400, Map.of("error", ex.getMessage()));
        }
    }
}
