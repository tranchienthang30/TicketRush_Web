package ticketrush;

public final class AdminMain {
    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(9004, new AdminService(new DataStore())::handle);
        System.out.println("Admin service listening on 9004");
    }
}
