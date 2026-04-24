package ticketrush;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public final class BookingService {
    private final DataStore store;

    public BookingService(DataStore store) {
        this.store = store;
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                HttpUtil.json(exchange, 404, Map.of("error", "Not found"));
                return;
            }
            int showId = Integer.parseInt(parts[3]);
            Map<String, Object> payload = ServiceParsers.parse(exchange);
            String clientId = HttpUtil.query(exchange.getRequestURI()).getOrDefault("client_id", ServiceParsers.str(payload.get("clientIdHeader")));
            String token = ServiceParsers.str(payload.get("token"));
            if (path.endsWith("/detail")) {
                HttpUtil.json(exchange, 200, store.showDetail(showId, clientId));
            } else if (path.endsWith("/hold")) {
                HttpUtil.json(exchange, 200, store.hold(showId, clientId, ServiceParsers.list(ServiceParsers.map(payload.get("body")).get("seatIds"))));
            } else if (path.endsWith("/checkout")) {
                HttpUtil.json(exchange, 200, store.checkout(showId, clientId, ServiceParsers.map(payload.get("body")), token));
            } else if (path.endsWith("/orders")) {
                HttpUtil.json(exchange, 200, store.orders(token, clientId));
            } else {
                HttpUtil.json(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception ex) {
            HttpUtil.json(exchange, 400, Map.of("error", ex.getMessage()));
        }
    }
}
