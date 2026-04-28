package ticketrush.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * DatabaseConnectionTest - Kiểm tra kết nối MongoDB
 * Chạy trực tiếp để test xem kết nối hoạt động không
 */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  MongoDB Connection Test");
        System.out.println("========================================\n");

        try {
            // Test 1: Khởi tạo kết nối
            System.out.println("Test 1: Khởi tạo kết nối...");
            DatabaseConnection.connect();
            Thread.sleep(500);

            // Test 2: Kiểm tra trạng thái kết nối
            System.out.println("\nTest 2: Kiểm tra trạng thái kết nối...");
            boolean isConnected = DatabaseConnection.isConnected();
            System.out.println("Kết nối đang hoạt động: " + (isConnected ? "✓ YES" : "✗ NO"));

            if (!isConnected) {
                System.err.println("✗ Lỗi: Kết nối thất bại. Kiểm tra MongoDB đang chạy không?");
                System.exit(1);
            }

            // Test 3: Lấy database instance
            System.out.println("\nTest 3: Lấy database instance...");
            MongoDatabase db = DatabaseConnection.getDatabase();
            System.out.println("✓ Database instance lấy thành công");
            System.out.println("  Database name: " + db.getName());

            // Test 4: Liệt kê các collections
            System.out.println("\nTest 4: Liệt kê các collections hiện có:");
            db.listCollectionNames().into(new java.util.ArrayList<String>())
                    .forEach(collectionName -> {
                        System.out.println("  - " + collectionName);
                    });

            // Test 5: Truy cập collection "movies"
            System.out.println("\nTest 5: Truy cập collection 'movies'...");
            MongoCollection<Document> moviesCollection = db.getCollection("movies");
            long movieCount = moviesCollection.countDocuments();
            System.out.println("✓ Collection 'movies' truy cập thành công");
            System.out.println("  Số lượng document: " + movieCount);

            // Test 6: Truy cập collection "showtimes"
            System.out.println("\nTest 6: Truy cập collection 'showtimes'...");
            MongoCollection<Document> showtimesCollection = db.getCollection("showtimes");
            long showtimeCount = showtimesCollection.countDocuments();
            System.out.println("✓ Collection 'showtimes' truy cập thành công");
            System.out.println("  Số lượng document: " + showtimeCount);

            // Test 7: Lấy document đầu tiên từ movies
            if (movieCount > 0) {
                System.out.println("\nTest 7: Lấy document đầu tiên từ 'movies'...");
                Document firstMovie = moviesCollection.find().first();
                if (firstMovie != null) {
                    System.out.println("✓ Thành công:");
                    System.out.println("  " + firstMovie.toJson());
                }
            }

            // Test 8: Insert test document
            System.out.println("\nTest 8: Test insert document vào collection tạm thời...");
            MongoCollection<Document> testCollection = db.getCollection("test");
            Document testDoc = new Document("name", "Test Connection")
                    .append("timestamp", System.currentTimeMillis())
                    .append("status", "active");
            testCollection.insertOne(testDoc);
            System.out.println("✓ Insert thành công");
            System.out.println("  Document: " + testDoc.toJson());

            // Test 9: Xóa test document
            System.out.println("\nTest 9: Xóa test document...");
            testCollection.deleteMany(new Document("name", "Test Connection"));
            System.out.println("✓ Xóa thành công");

            // Summary
            System.out.println("\n========================================");
            System.out.println("  ✓ TẤT CẢ CÁC TEST PASSED!");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("\n✗ LỖI TRONG TEST:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Đóng kết nối
            System.out.println("Đóng kết nối...");
            DatabaseConnection.close();
        }
    }
}
