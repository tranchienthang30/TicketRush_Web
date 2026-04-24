package ticketrush;

public final class AuthMain {
    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(9001, new AuthService(new DataStore())::handle);
        System.out.println("Auth service listening on 9001");
    }
}
