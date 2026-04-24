package ticketrush;

public final class BookingMain {
    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(9003, new BookingService(new DataStore())::handle);
        System.out.println("Booking service listening on 9003");
    }
}
