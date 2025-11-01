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

    /**
     * Bắt đầu một trận đấu giữa hai người chơi.
     * - Tạo bản ghi mới trong match_history với start_date = NOW()
     * - end_date, score, result để NULL cho đến khi kết thúc.
     */
    public static DatabaseResponse<Integer> startMatch(String username, String opponentName) {
        System.out.println("=== [DEBUG] GỌI startMatch() ===");
        System.out.println("  → username: " + username);
        System.out.println("  → opponentName: " + opponentName);

        String findUserSql = "SELECT id FROM users WHERE username = ?";
        String insertHistorySql = "INSERT INTO match_history (user_id, opponent_id) VALUES (?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            int userId = -1;
            int opponentId = -1;

            // 🔹 Lấy user_id
            try (PreparedStatement pstmt = conn.prepareStatement(findUserSql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) userId = rs.getInt("id");
                else return DatabaseResponse.error("Không tìm thấy người chơi: " + username);
            }

            // 🔹 Lấy opponent_id
            if (username.equals(opponentName)) {
                opponentId = userId;
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(findUserSql)) {
                    pstmt.setString(1, opponentName);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) opponentId = rs.getInt("id");
                    else return DatabaseResponse.error("Không tìm thấy đối thủ: " + opponentName);
                }
            }

            // 🔹 Thêm bản ghi match_history và lấy match_id
            int matchId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(insertHistorySql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, opponentId);
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) matchId = rs.getInt(1);
            }

            conn.commit();
            System.out.println("=== [DEBUG] MATCH_ID tạo ra: " + matchId + " ===");

            return DatabaseResponse.success(matchId, "✅ Bắt đầu trận đấu thành công — match_id: " + matchId);

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("❌ Lỗi khi bắt đầu trận đấu: " + e.getMessage());
        }
    }





    /**
     * Cập nhật điểm và lịch sử trận đấu khi trận kết thúc.
     * - Cập nhật match_history (score, result, end_date)
     * - Cập nhật high_score nếu cần
     */
    /**
     * Cập nhật điểm và kết quả cho 1 bản ghi match_history cụ thể (dựa vào match_id).
     */
    public static DatabaseResponse<Void> updateScoreAfterMatch(int matchId, String username, String result) {
        System.out.println("[DEBUG] updateScoreAfterMatch called → matchId=" + matchId + ", username=" + username + ", result=" + result);

        String findUserSql = "SELECT id, high_score FROM users WHERE username = ?";
        String updateHistorySql = "UPDATE match_history SET score = ?, result = ?, end_date = NOW() WHERE id = ?";
        String updateHighScoreSql = "UPDATE users SET high_score = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // 1️⃣ Lấy user_id, current_score, high_score
            int userId;
            int currentScore;
            int currentHighScore;

            DatabaseResponse<Integer> scoreRes = getUserScore(username);
            if (!scoreRes.isSuccess()) {
                return DatabaseResponse.error("Không thể lấy điểm hiện tại: " + scoreRes.getMessage());
            }
            currentScore = scoreRes.getData();

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

            // 2️⃣ Tính điểm mới
            int newScore;
            switch (result.toLowerCase()) {
                case "win":
                    newScore = currentScore + 10;
                    break;
                case "lose":
                    newScore = Math.max(0, currentScore - 5);
                    break;
                case "draw":
                    newScore = currentScore + 5;
                    break;
                default:
                    return DatabaseResponse.error("Kết quả không hợp lệ: " + result);
            }

            // 3️⃣ Cập nhật match_history dựa theo match_id
            try (PreparedStatement pstmt = conn.prepareStatement(updateHistorySql)) {
                pstmt.setInt(1, newScore);
                pstmt.setString(2, result.toLowerCase());
                pstmt.setInt(3, matchId);
                int updated = pstmt.executeUpdate();

                if (updated == 0) {
                    conn.rollback();
                    return DatabaseResponse.error("❌ Không tìm thấy trận đấu có match_id: " + matchId);
                }
            }

            // 4️⃣ Cập nhật high_score nếu vượt qua điểm cao nhất
            if (newScore > currentHighScore) {
                try (PreparedStatement pstmt = conn.prepareStatement(updateHighScoreSql)) {
                    pstmt.setInt(1, newScore);
                    pstmt.setInt(2, userId);
                    pstmt.executeUpdate();
                }
            }

            conn.commit();
            return DatabaseResponse.success("✅ Cập nhật kết quả và điểm thành công.");

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("❌ Lỗi cơ sở dữ liệu khi cập nhật trận đấu: " + e.getMessage());
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
                "  h.start_date, " +
                "  h.end_date " +
                "FROM match_history h " +
                "JOIN users u ON h.user_id = u.id " +
                "JOIN users o ON h.opponent_id = o.id " +
                "WHERE h.user_id = ? " +
                "AND h.end_date IS NOT NULL " +
                "AND h.result IS NOT NULL " +   // ✅ thêm khoảng trắng ở đây
                "ORDER BY h.start_date DESC";





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
                    String playedAt = rs.getString("start_date");
                    String endDate = rs.getString("end_date");

                    historyList.add(new MatchHistory(myName, opponentName, result, playedAt, endDate));
                }
            }

            return DatabaseResponse.success(historyList);

        } catch (Exception e) {
            e.printStackTrace();
            return DatabaseResponse.failure("Lỗi khi truy vấn lịch sử đấu: " + e.getMessage());
        }
    }

    public static DatabaseResponse<List<UserProfile>> getAllUsers() {
        List<UserProfile> users = new ArrayList<>();
        String sql = "SELECT id, username, email, high_score FROM users";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UserProfile user = new UserProfile();
                user.setUserId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setScore(rs.getInt("high_score"));
                users.add(user);
            }

            return DatabaseResponse.success(users, "Lay DS nguoi choi thanh cong.");

        } catch (SQLException e) {
            e.printStackTrace();
            return DatabaseResponse.error("Loi CSDL khi lấy DS người chơi: " + e.getMessage());
        }
    }

}