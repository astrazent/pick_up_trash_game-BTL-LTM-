package server.data;

// Lớp này phải khớp với cấu trúc JSON mà server gửi về
public class MatchHistory {
    private String myName;
    private String opponentName;
    private String result; // Ví dụ: "WIN", "LOSS", "DRAW"
    private String gameDate; // Ví dụ: "2025-10-21"

    // Constructor
    public MatchHistory(String myName, String opponentName, String result, String gameDate) {
        this.myName = myName;
        this.opponentName = opponentName;
        this.result = result;
        this.gameDate = gameDate;
    }

    // Getters
    public String getMyName() {
        return myName;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public String getResult() {
        return result;
    }

    public String getGameDate() {
        return gameDate;
    }
}
