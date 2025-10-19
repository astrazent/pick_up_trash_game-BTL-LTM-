// Đặt file này trong package gamePlay.utils hoặc một package data mới
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

    /**
     * Tạo một response thành công.
     */
    public static <T> DatabaseResponse<T> success(T data, String message) {
        return new DatabaseResponse<>(true, message, data);
    }

    /**
     * Tạo một response thành công không có dữ liệu trả về.
     */
    public static <T> DatabaseResponse<T> success(String message) {
        return new DatabaseResponse<>(true, message, null);
    }

    /**
     * Tạo một response thất bại.
     */
    public static <T> DatabaseResponse<T> error(String message) {
        return new DatabaseResponse<>(false, message, null);
    }
}