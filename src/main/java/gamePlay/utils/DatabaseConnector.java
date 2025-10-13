package gamePlay.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector {

    // Thay đổi các thông số này để phù hợp với cài đặt MySQL của bạn
    private static final String DB_URL = "jdbc:mysql://localhost:55566/pick_up_trash";
    private static final String USER = "root"; // Thay bằng username của bạn
    private static final String PASS = "55566"; // Thay bằng password của bạn

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    // Kiểm tra thông tin đăng nhập
    public static boolean validateUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return storedPassword.equals(password);
            }
            return false; // Không tìm thấy user

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Lưu điểm của người chơi vào leaderboard
    public static void saveScore(String username, int score) {
        String sql = "INSERT INTO leaderboard (username, score) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lấy danh sách điểm cao (ví dụ: top 10)
    public static List<String> getLeaderboard() {
        List<String> leaderboard = new ArrayList<>();
        String sql = "SELECT username, score, game_date FROM leaderboard ORDER BY score DESC LIMIT 10";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String entry = String.format("%-20s %-10d %s",
                        rs.getString("username"),
                        rs.getInt("score"),
                        rs.getTimestamp("game_date").toLocalDateTime().toString()
                );
                leaderboard.add(entry);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leaderboard;
    }
}