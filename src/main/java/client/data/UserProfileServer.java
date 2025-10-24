package client.data;

import java.io.Serializable;

// Class này nên tồn tại cả ở project Server và Client để đồng bộ
public class UserProfileServer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String email;
    private int score; // Hoặc high_score, level...

    // Constructor rỗng
    public UserProfileServer() {
    }

    // Constructor đầy đủ
    public UserProfileServer(int userId, String username, String email, int score) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.score = score;
    }

    // --- Getters and Setters ---
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    @Override
    public String toString() {
        return "UserProfile{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", score=" + score +
                '}';
    }
}