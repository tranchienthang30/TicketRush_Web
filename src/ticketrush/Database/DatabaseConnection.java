package ticketrush.Database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

/**
 * DatabaseConnection - Kết nối tới MongoDB
 * Quản lý connection pool và cấp quyền truy cập database
 */
public class DatabaseConnection {
    
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static final String DATABASE_NAME = "movie_booking";
    
    /**
     * Sample MongoDB URIs:
     * 
     * 1. Local Development:
     *    mongodb://localhost:27017/movie_booking
     * 
     * 2. With Authentication:
     *    mongodb://username:password@localhost:27017/movie_booking
     * 
     * 3. MongoDB Atlas Cloud:
     *    mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/movie_booking?retryWrites=true&w=majority
     * 
     * 4. Replica Set:
     *    mongodb://host1:27017,host2:27017,host3:27017/movie_booking?replicaSet=rs0
     */
    
    // Sample URI cho Local Development
    private static final String MONGODB_URI = "mongodb+srv://ntpjr:ntpjr@movie.lsoxb7m.mongodb.net/";
    
    /**
     * Khởi tạo kết nối MongoDB
     */
    public static void connect() {
        try {
            MongoClientURI uri = new MongoClientURI(MONGODB_URI);
            mongoClient = new MongoClient(uri);
            database = mongoClient.getDatabase(DATABASE_NAME);
            System.out.println("✓ Connected to MongoDB: " + DATABASE_NAME);
        } catch (Exception e) {
            System.err.println("✗ Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy instance MongoDatabase
     */
    public static MongoDatabase getDatabase() {
        if (database == null) {
            connect();
        }
        return database;
    }
    
    /**
     * Lấy instance MongoClient
     */
    public static MongoClient getMongoClient() {
        if (mongoClient == null) {
            connect();
        }
        return mongoClient;
    }
    
    /**
     * Đóng kết nối
     */
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("✓ MongoDB connection closed");
        }
    }
    
    /**
     * Kiểm tra trạng thái kết nối
     */
    public static boolean isConnected() {
        try {
            database.listCollectionNames().first();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
