package server.network;

// Import các lớp cần thiết
import com.google.gson.Gson;
import server.data.MatchHistory;
import server.data.UserProfile;
import server.data.UserProfileServer;
import server.network.GameRoom;
import server.network.GameServer;
import server.utils.DatabaseConnector;
import server.utils.DatabaseResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ClientTCPHandler implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private UserProfileServer userProfile; // Lưu trữ toàn bộ thông tin user
    private InetSocketAddress udpAddress;
    private GameRoom currentRoom = null;

    public ClientTCPHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    public void setCurrentRoom(GameRoom room) { this.currentRoom = room; }
    public GameRoom getCurrentRoom() { return this.currentRoom; }
    public void setUdpAddress(InetSocketAddress address) { this.udpAddress = address; }
    public InetSocketAddress getUdpAddress() { return this.udpAddress; }
    public String getUsername() { return this.username; }
    public UserProfileServer getUserProfile() { return this.userProfile; } // Getter cho profile

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Nhan tu client " + clientSocket.getInetAddress() + ": " + inputLine);
                handleClientMessage(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Client đã ngắt kết nối: " + (username != null ? username : clientSocket.getInetAddress()));
        } finally {
            server.removeClient(this); // Báo cho server biết client đã rời
            server.broadcastOnlineList();
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    public void cleanup() {
        System.out.println("Dọn dẹp tài nguyên cho client: " + (username != null ? username : "UNKNOWN"));

        // Đóng socket
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                in.close();
                out.close();
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng socket cho " + username + ": " + e.getMessage());
        }

        System.out.println("→ Đã cleanup xong cho " + (username != null ? username : "UNKNOWN"));
    }

    public boolean isOnline(){
        return server.isOnline(this);
    }

    private void handleClientMessage(String message) {
        // Ưu tiên xử lý tin nhắn trong game nếu đã vào phòng
        if (currentRoom != null && !message.startsWith("LOGIN") && !message.startsWith("READY") && !message.startsWith("GET_ALL_USERS")) {
            currentRoom.handleGameMessage(message, this.username);
            return;
        }

        String[] parts = message.split(";", 2); // Tách command và phần còn lại
        String command = parts[0];

        switch (command) {
            case "LOGIN":
                // Message format: "LOGIN;username;password"
                String[] loginParts = message.split(";");
                if (loginParts.length == 3) {
                    String user = loginParts[1];
                    String pass = loginParts[2];

                    // Gọi phương thức validateUser đã được nâng cấp
                    DatabaseResponse<UserProfileServer> response = DatabaseConnector.validateUser(user, pass);

                    if (response.isSuccess()) {
                        this.userProfile = response.getData();
                        this.username = this.userProfile.getUsername();

                        // Chuyển đối tượng UserProfile thành JSON để gửi đi
                        Gson gson = new Gson();
                        String profileJson = gson.toJson(this.userProfile);

                        // Gửi thông báo thành công kèm theo dữ liệu JSON
                        sendMessage("LOGIN_SUCCESS;" + profileJson);
                        server.broadcastOnlineList();
                    } else {
                        // Gửi thông báo thất bại kèm theo lý do từ DatabaseResponse
                        sendMessage("LOGIN_FAILED;" + response.getMessage());
                    }
                }
                break;

            case "READY":
                // Message format: "READY;username;playerCount" hoặc "READY;username"
                String[] readyParts = message.split(";");
                if (username == null) {
                    sendMessage("ERROR;Bạn cần đăng nhập trước khi sẵn sàng.");
                    return;
                }
                if (readyParts.length == 3) {
                    int playerCount = Integer.parseInt(readyParts[2]);
                    server.playerIsReady(this, playerCount);
                } else if (readyParts.length == 2) {
                    server.playerIsReady(this, 2); // Mặc định là 2 người chơi
                }
                break;

            case "SAVE_SCORE":
                // Message format: "SAVE_SCORE;username;score"
                String[] scoreParts = message.split(";");
                if (scoreParts.length == 3) {
                    String scoreUser = scoreParts[1];
                    int score = Integer.parseInt(scoreParts[2]);

                    // Gọi phương thức saveScore đã được nâng cấp
                    DatabaseResponse<Void> response = DatabaseConnector.saveScore(scoreUser, score);

                    if (response.isSuccess()) {
                        sendMessage("SCORE_SAVED_SUCCESS;" + response.getMessage());
                    } else {
                        sendMessage("SCORE_SAVED_FAILED;" + response.getMessage());
                    }
                }
                break;

            // Bạn có thể thêm các command khác ở đây, ví dụ: lấy bảng xếp hạng
             case "GET_LEADERBOARD":
                 DatabaseResponse<List<UserProfile>> leaderboardResponse = DatabaseConnector.getLeaderboard();
                 if (leaderboardResponse.isSuccess()) {
                     Gson gson = new Gson();
                     String leaderboardJson = gson.toJson(leaderboardResponse.getData());
                     sendMessage("LEADERBOARD_DATA;" + leaderboardJson);
                 } else {
                     sendMessage("LEADERBOARD_FAILED;" + leaderboardResponse.getMessage());
                 }
                 break;
            case "GET_HISTORY":
                // Gọi phương thức từ DatabaseConnector với ID của người dùng hiện tại
                DatabaseResponse<List<MatchHistory>> historyResponse = DatabaseConnector.getMatchHistory(username);

                if (historyResponse.isSuccess()) {
                    Gson gson = new Gson();
                    String historyJson = gson.toJson(historyResponse.getData());
                    sendMessage("HISTORY_DATA;" + historyJson);
                } else {
                    sendMessage("HISTORY_FAILED;" + historyResponse.getMessage());
                }
                break;
            case "GET_ALL_USERS":
                DatabaseResponse<List<UserProfile>> usersResponse = DatabaseConnector.getAllUsers();
                if (usersResponse.isSuccess()) {
                    Gson gson = new Gson();
                    String usersJson = gson.toJson(usersResponse.getData());
                    sendMessage("ALL_USERS_DATA;" + usersJson);
                } else {
                    sendMessage("ALL_USERS_FAILED;" + usersResponse.getMessage());
                    System.err.println("Ko the lay DS nguoi choi: " + usersResponse.getMessage());
                }
                break;

        }
    }
}