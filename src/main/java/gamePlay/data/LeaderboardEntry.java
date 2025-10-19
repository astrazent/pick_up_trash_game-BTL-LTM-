// Đặt file này trong package gamePlay.data
package gamePlay.data;

import java.time.LocalDateTime;

public class LeaderboardEntry {
    private String username;
    private int score;
    private LocalDateTime gameDate;

    public LeaderboardEntry(String username, int score, LocalDateTime gameDate) {
        this.username = username;
        this.score = score;
        this.gameDate = gameDate;
    }

    // Thêm getters nếu cần thiết để Gson có thể serialize
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public LocalDateTime getGameDate() { return gameDate; }
}