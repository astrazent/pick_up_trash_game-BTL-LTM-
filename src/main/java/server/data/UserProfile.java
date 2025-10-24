package server.data;

/**
 * Lớp này đại diện cho thông tin cá nhân của người dùng.
 * Nó được sử dụng để hứng dữ liệu JSON được gửi từ server sau khi đăng nhập thành công.
 * Đây là một POJO (Plain Old Java Object) đơn giản.
 */
public class UserProfile {

    // Tên các thuộc tính này PHẢI KHỚP với các key trong JSON mà server gửi về.
    // Ví dụ JSON: {"userId":1, "username":"player1", "email":"p1@game.com", "score":100}
    private int userId;
    private String username;
    private String email;
    private int score;

    /**
     * Constructor rỗng.
     * Rất quan trọng! Các thư viện như Gson cần nó để có thể tạo một đối tượng rỗng
     * trước khi điền dữ liệu từ JSON vào.
     */
    public UserProfile() {
    }

    // --- GETTERS ---
    // Cần thiết để các phần khác của ứng dụng có thể đọc thông tin.

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public int getScore() {
        return score;
    }

    // --- SETTERS ---
    // Hữu ích nếu bạn muốn cập nhật dữ liệu ở phía client (ví dụ: cập nhật điểm).

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Phương thức toString() giúp cho việc debug dễ dàng hơn.
     * Khi bạn in đối tượng này ra console, nó sẽ hiển thị nội dung thay vì địa chỉ bộ nhớ.
     */
    @Override
    public String toString() {
        return "UserProfile{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ",highScore=" + score +
                '}';
    }
}