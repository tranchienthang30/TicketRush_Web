package ticketrush;

import java.nio.file.Path;

public final class GatewayMain {
    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(8000, new GatewayService(Path.of("static"))::handle);
        System.out.println("Gateway listening on 8000");
    }
}
