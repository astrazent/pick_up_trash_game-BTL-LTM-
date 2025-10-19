package gamePlay.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import gamePlay.data.LeaderboardEntry; // Import lớp mới
import gamePlay.data.UserProfileServer; // Đảm bảo bạn đã có class này

public class DatabaseConnector {

    private static final String DB_URL = "jdbc:mysql://localhost:55566/pick_up_trash";
    private static final String USER = "root";
    private static final String PASS = "55566";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    /**
     * Xác thực người dùng và trả về thông tin profile.
     * @return DatabaseResponse chứa UserProfileServer nếu thành công.
     */
    public static DatabaseResponse<UserProfileServer> validateUser(String username, String password) {
        // Cần lấy tất cả thông tin user, không chỉ password
        String sql = "SELECT id, username, email, high_score, password FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (storedPassword.equals(password)) {
                    // Mật khẩu đúng, tạo đối tượng UserProfileServer
                    int id = rs.getInt("id");
                    String user = rs.getString("username");
                    String email = rs.getString("email");
                    int score = rs.getInt("high_score"); // ✅ Sửa lại đúng tên cột

                    UserProfileServer userProfile = new UserProfileServer(id, user, email, score);
                    return DatabaseResponse.success(userProfile, "Đăng nhập thành công.");
                }
            }
            // Không tìm thấy user
            return DatabaseResponse.error("Tên đăng nhập không tồn tại.");

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Lỗi cơ sở dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Lưu điểm của người chơi.
     * @return DatabaseResponse chứa trạng thái thành công/thất bại.
     */
    public static DatabaseResponse<Void> saveScore(String username, int score) {
        String sql = "INSERT INTO leaderboard (username, score) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setInt(2, score);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                return DatabaseResponse.success("Lưu điểm thành công.");
            } else {
                return DatabaseResponse.error("Không thể lưu điểm.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Lỗi cơ sở dữ liệu khi lưu điểm: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách điểm cao.
     * @return DatabaseResponse chứa danh sách LeaderboardEntry.
     */
    public static DatabaseResponse<List<LeaderboardEntry>> getLeaderboard() {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String sql = "SELECT username, score, game_date FROM leaderboard ORDER BY score DESC LIMIT 10";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                LeaderboardEntry entry = new LeaderboardEntry(
                        rs.getString("username"),
                        rs.getInt("score"),
                        rs.getTimestamp("game_date").toLocalDateTime()
                );
                leaderboard.add(entry);
            }
            return DatabaseResponse.success(leaderboard, "Lấy bảng xếp hạng thành công.");

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Lỗi cơ sở dữ liệu khi lấy bảng xếp hạng: " + e.getMessage());
        }
    }
}