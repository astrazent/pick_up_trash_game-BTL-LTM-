package server.utils;

import server.data.MatchHistory;
import server.data.UserProfile;
import server.data.UserProfileServer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DatabaseConnector {

    private static final String DB_URL = "jdbc:mysql://localhost:55566/pick_up_trash?serverTimezone=Asia/Ho_Chi_Minh";
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
     * Lấy điểm cao nhất của một người chơi
     * @param username Tên người chơi cần lấy điểm.
     * @return DatabaseResponse chứa điểm cao nhất (Integer).
     */
    public static DatabaseResponse<Integer> getUserScore(String username) {
        String sql = """
        SELECT high_score
        FROM users
        WHERE username = ?
    """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int score = rs.getInt("high_score");
                return DatabaseResponse.success(score, "Lấy điểm cao nhất thành công.");
            } else {
                return DatabaseResponse.error("Không tìm thấy người chơi: " + username);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Lỗi cơ sở dữ liệu khi lấy điểm: " + e.getMessage());
        }
    }
    /**
     * Cập nhật điểm của người chơi:
     * - Nếu điểm mới cao hơn high_score trong bảng users, thì cập nhật high_score.
     * @param username Tên người chơi.
     * @param newScore Điểm mới đạt được.
     * @return DatabaseResponse chứa thông báo thành công hoặc lỗi.
     */
    public static DatabaseResponse<Void> updateUserScore(String username, int newScore) {
        String findUserSql = "SELECT id, high_score FROM users WHERE username = ?";
        String updateHighScoreSql = "UPDATE users SET high_score = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            int userId = -1;
            int currentHighScore = 0;

            // 1️⃣ Lấy thông tin người chơi
            try (PreparedStatement pstmt = conn.prepareStatement(findUserSql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("id");
                    currentHighScore = rs.getInt("high_score");
                } else {
                    return DatabaseResponse.error("Không tìm thấy người chơi: " + username);
                }
            }

            // 2️⃣ Nếu điểm mới cao hơn high_score → cập nhật
            if (newScore > currentHighScore) {
                try (PreparedStatement pstmt = conn.prepareStatement(updateHighScoreSql)) {
                    pstmt.setInt(1, newScore);
                    pstmt.setInt(2, userId);
                    pstmt.executeUpdate();
                }
            }

            conn.commit();
            return DatabaseResponse.success("Cập nhật điểm thành công.");

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Lỗi cơ sở dữ liệu khi cập nhật điểm: " + e.getMessage());
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
     * Cập nhật điểm của người chơi sau khi trận đấu kết thúc.
     * - Tính điểm mới dựa trên kết quả: thắng, thua, hòa.
     * - Lưu trận đấu vào bảng match_history.
     * - Cập nhật high_score trong users nếu điểm mới cao hơn.
     *
     * @param username Tên người chơi.
     * @param result Kết quả trận đấu: "win", "lose", "draw".
     * @return DatabaseResponse chứa thông báo thành công hoặc lỗi.
     */
    public static DatabaseResponse<Void> updateScoreAfterMatch(String username, String opponentName, String result) {
        DatabaseResponse<Integer> currentScoreResponse = getUserScore(username);
        if (!currentScoreResponse.isSuccess()) {
            return DatabaseResponse.error("Không thể lấy điểm người chơi: " + currentScoreResponse.getMessage());
        }

        int currentScore = currentScoreResponse.getData();
        int newScore = currentScore;

        switch (result.toLowerCase()) {
            case "win":
                newScore += 10;
                break;
            case "lose":
                newScore = Math.max(0, currentScore - 5);
                break;
            case "draw":
                newScore += 5;
                break;
            default:
                return DatabaseResponse.error("Kết quả không hợp lệ: " + result);
        }

        String findUserSql = "SELECT id, high_score FROM users WHERE username = ?";
        String findOpponentSql = "SELECT id FROM users WHERE username = ?";
        String insertHistorySql = "INSERT INTO match_history (user_id, opponent_id, score, result) VALUES (?, ?, ?, ?)";
        String updateHighScoreSql = "UPDATE users SET high_score = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            int userId;
            int opponentId;
            int currentHighScore;

            // Lấy thông tin người chơi hiện tại
            try (PreparedStatement pstmt = conn.prepareStatement(findUserSql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("id");
                    currentHighScore = rs.getInt("high_score");
                } else {
                    return DatabaseResponse.error("Không tìm thấy người chơi: " + username);
                }
            }

            // Lấy opponent_id từ opponentName
            try (PreparedStatement pstmt = conn.prepareStatement(findOpponentSql)) {
                pstmt.setString(1, opponentName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    opponentId = rs.getInt("id");
                } else {
                    return DatabaseResponse.error("Không tìm thấy đối thủ: " + opponentName);
                }
            }

            // Lưu lịch sử trận đấu
            try (PreparedStatement pstmt = conn.prepareStatement(insertHistorySql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, opponentId);
                pstmt.setInt(3, newScore);
                pstmt.setString(4, result.toLowerCase());
                pstmt.executeUpdate();
            }

            // Cập nhật high_score nếu cần
            if (newScore > currentHighScore) {
                try (PreparedStatement pstmt = conn.prepareStatement(updateHighScoreSql)) {
                    pstmt.setInt(1, newScore);
                    pstmt.setInt(2, userId);
                    pstmt.executeUpdate();
                }
            }

            conn.commit();
            return DatabaseResponse.success("Cập nhật điểm và lưu trận đấu thành công.");

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Lỗi cơ sở dữ liệu khi cập nhật điểm: " + e.getMessage());
        }
    }
    /**
     * Lấy danh sách điểm cao từ users.high_score.
     * Sử dụng UserProfile thay cho LeaderboardEntry.
     * @return DatabaseResponse chứa danh sách UserProfile.
     */
    public static DatabaseResponse<List<UserProfile>> getLeaderboard() {
        List<UserProfile> leaderboard = new ArrayList<>();
        String sql = "SELECT id, username, high_score FROM users ORDER BY high_score DESC LIMIT 10";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UserProfile user = new UserProfile();
                user.setUserId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setScore(rs.getInt("high_score"));

                leaderboard.add(user);
            }
            return DatabaseResponse.success(leaderboard, "Lấy bảng xếp hạng thành công.");

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Lỗi cơ sở dữ liệu khi lấy bảng xếp hạng: " + e.getMessage());
        }
    }
    public static DatabaseResponse<List<MatchHistory>> getMatchHistory(String username) {
        List<MatchHistory> historyList = new ArrayList<>();

        String getUserIdSql = "SELECT id FROM users WHERE username = ?";
        String sql = "SELECT " +
                "  u.username AS my_name, " +
                "  o.username AS opponent_name, " +
                "  h.result, " +
                "  h.played_at " +
                "FROM match_history h " +
                "JOIN users u ON h.user_id = u.id " +
                "JOIN users o ON h.opponent_id = o.id " +
                "WHERE h.user_id = ? " +
                "ORDER BY h.played_at DESC";

        try (Connection conn = getConnection()) {
            int userId;

            // 1️⃣ Lấy user_id từ username
            try (PreparedStatement stmt = conn.prepareStatement(getUserIdSql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("id");
                } else {
                    return DatabaseResponse.failure("Không tìm thấy người dùng: " + username);
                }
            }

            // 2️⃣ Lấy lịch sử đấu theo user_id
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String myName = rs.getString("my_name");
                    String opponentName = rs.getString("opponent_name");
                    String result = rs.getString("result").toUpperCase();
                    String playedAt = rs.getString("played_at");

                    historyList.add(new MatchHistory(myName, opponentName, result, playedAt));
                }
            }

            return DatabaseResponse.success(historyList);

        } catch (Exception e) {
            e.printStackTrace();
            return DatabaseResponse.failure("Lỗi khi truy vấn lịch sử đấu: " + e.getMessage());
        }
    }
}