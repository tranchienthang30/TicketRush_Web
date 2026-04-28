package ticketrush.services.payment;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * PaymentService - CRUD operations cho collection "payments"
 */
public class PaymentService {

    private static final String COLLECTION_NAME = "payments";

    /**
     * Lấy MongoCollection payments
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới giao dịch thanh toán
     */
    public static ObjectId createPayment(String bookingId, String userId, double amount,
            String paymentMethod, String status) {
        try {
            Document payment = new Document()
                    .append("booking_id", new ObjectId(bookingId))
                    .append("user_id", new ObjectId(userId))
                    .append("amount", amount)
                    .append("payment_method", paymentMethod) // credit_card, debit_card, vnpay, paypal...
                    .append("status", status) // pending, success, failed, refunded
                    .append("transaction_id", "TXN_" + System.currentTimeMillis())
                    .append("payment_date", System.currentTimeMillis());

            getCollection().insertOne(payment);
            return payment.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo thanh toán: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy thanh toán theo ID
     */
    public static Document getPaymentById(String paymentId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(paymentId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy thanh toán: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả thanh toán
     */
    public static List<Document> getAllPayments() {
        List<Document> payments = new ArrayList<>();
        try {
            getCollection().find().into(payments);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách thanh toán: " + e.getMessage());
        }
        return payments;
    }

    /**
     * Lấy thanh toán theo đơn đặt vé
     */
    public static Document getPaymentByBooking(String bookingId) {
        try {
            return getCollection().find(Filters.eq("booking_id", new ObjectId(bookingId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy thanh toán theo booking: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy thanh toán theo người dùng
     */
    public static List<Document> getPaymentsByUser(String userId) {
        List<Document> payments = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("user_id", new ObjectId(userId))).into(payments);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy thanh toán theo người dùng: " + e.getMessage());
        }
        return payments;
    }

    /**
     * Lấy thanh toán theo trạng thái
     */
    public static List<Document> getPaymentsByStatus(String status) {
        List<Document> payments = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("status", status)).into(payments);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy thanh toán theo trạng thái: " + e.getMessage());
        }
        return payments;
    }

    /**
     * Lấy thanh toán theo phương thức
     */
    public static List<Document> getPaymentsByMethod(String paymentMethod) {
        List<Document> payments = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("payment_method", paymentMethod)).into(payments);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy thanh toán theo phương thức: " + e.getMessage());
        }
        return payments;
    }

    /**
     * Cập nhật trạng thái thanh toán
     */
    public static boolean updatePaymentStatus(String paymentId, String newStatus) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(paymentId)),
                    Updates.set("status", newStatus));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật trạng thái thanh toán: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật ID giao dịch
     */
    public static boolean updateTransactionId(String paymentId, String transactionId) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(paymentId)),
                    Updates.set("transaction_id", transactionId));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật ID giao dịch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa thanh toán
     */
    public static boolean deletePayment(String paymentId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(paymentId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa thanh toán: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số thanh toán
     */
    public static long countPayments() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm thanh toán: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Tính tổng doanh thu
     */
    public static double getTotalRevenue() {
        try {
            return getCollection().find(Filters.eq("status", "success"))
                    .into(new ArrayList<>()).stream()
                    .mapToDouble(doc -> doc.getDouble("amount"))
                    .sum();
        } catch (Exception e) {
            System.err.println("✗ Lỗi tính tổng doanh thu: " + e.getMessage());
            return 0;
        }
    }
}
