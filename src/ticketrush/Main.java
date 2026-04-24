package ticketrush;

import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws Exception {
        DataStore store = new DataStore();
        ServiceRuntime.start(9001, new AuthService(store)::handle);
        ServiceRuntime.start(9002, new CatalogService(store)::handle);
        ServiceRuntime.start(9003, new BookingService(store)::handle);
        ServiceRuntime.start(9004, new AdminService(store)::handle);
        ServiceRuntime.start(8000, new GatewayService(Path.of("static"))::handle);
        System.out.println("TicketRush suite running at http://127.0.0.1:8000");
    }
}
