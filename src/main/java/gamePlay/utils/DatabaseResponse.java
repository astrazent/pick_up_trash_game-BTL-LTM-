package gamePlay.utils;

public class DatabaseResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    // Constructor private để ép buộc sử dụng static factory methods
    private DatabaseResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // --- Getters ---
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    // --- Static Factory Methods ---

    /** ✅ Thành công (trả về data, không cần message) */
    public static <T> DatabaseResponse<T> success(T data) {
        return new DatabaseResponse<>(true, null, data);
    }

    /** Thành công (trả về data và message) */
    public static <T> DatabaseResponse<T> success(T data, String message) {
        return new DatabaseResponse<>(true, message, data);
    }

    /** Thành công (không có data, chỉ có message) */
    public static <T> DatabaseResponse<T> success(String message) {
        return new DatabaseResponse<>(true, message, null);
    }

    /** ✅ Thất bại (trả về message, data = null) */
    public static <T> DatabaseResponse<T> failure(String message) {
        return new DatabaseResponse<>(false, message, null);
    }

    /** Giữ lại alias cho error nếu bạn muốn */
    public static <T> DatabaseResponse<T> error(String message) {
        return failure(message);
    }
}
