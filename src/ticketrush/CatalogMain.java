package ticketrush;

public final class CatalogMain {
    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(9002, new CatalogService(new DataStore())::handle);
        System.out.println("Catalog service listening on 9002");
    }
}
