package client.network;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import client.Main;
import com.google.gson.stream.JsonReader;
import server.config.NetworkConfig;
import client.data.MatchHistory;
import client.data.UserProfile;
import client.game.Player;
import client.game.Trash;
import client.game.TrashType;
import client.scenes.GameScene;
import client.scenes.HistoryScene;
import client.scenes.LeaderboardScene;
import javafx.application.Platform;

import java.io.*;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {
    private static Client instance;
    private final String host;
    private final int tcpPort;
    private final int udpPort;

    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private PrintWriter tcpOut;
    private BufferedReader tcpIn;

    private UserProfile userProfile;

    private static final Gson gson = new Gson(); // Chỉ cần một Gson instance

    private Client(NetworkConfig config) {
        this.host = config.server.host;
        this.tcpPort = config.server.tcp_port;
        this.udpPort = config.server.udp_port;
    }

    public static synchronized void initialize(NetworkConfig config) {
        if (instance == null) {
            instance = new Client(config);
        }
    }

    public static synchronized Client getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Client chưa được khởi tạo. Hãy gọi initialize() trước.");
        }
        return instance;
    }

    public void connect() throws IOException {
        tcpSocket = new Socket(host, tcpPort);
        udpSocket = new DatagramSocket();
        tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
        tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

        new Thread(this::listenToServerTCP).start();
        new Thread(this::listenToServerUDP).start();
    }

    private void listenToServerTCP() {
        try {
            String serverMessage;
            while ((serverMessage = tcpIn.readLine()) != null) {
                System.out.println("Nhận TCP từ Server: " + serverMessage);

                // 🔍 Nếu dữ liệu là JSON (bắt đầu bằng { ), parse trực tiếp
                if (serverMessage.trim().startsWith("{")) {
                    handleJsonMessage(serverMessage);
                } else {
                    // ⚙️ Cũ: dạng text tách bằng dấu ";"
                    final String[] parts = serverMessage.split(";");
                    Platform.runLater(() -> handleServerMessage(parts));
                }
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối TCP với server.");
        }
    }

    private void handleJsonMessage(String json) {
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            String type = obj.get("type").getAsString();

            if ("HISTORY_DATA".equals(type)) {
                JsonArray historyJsonArray = obj.getAsJsonArray("data");
                Type matchHistoryListType = new TypeToken<List<client.data.MatchHistory>>() {}.getType();
                List<client.data.MatchHistory> historyData = gson.fromJson(historyJsonArray, matchHistoryListType);

                Platform.runLater(() -> {
                    HistoryScene scene = Main.getInstance().getActiveHistoryScene();
                    if (scene != null) {
                        scene.updateHistory(historyData);
                        System.out.println("✅ Đã cập nhật danh sách lịch sử đấu (" + historyData.size() + " trận)");
                    }
                });
            }
            else if ("LEADERBOARD_DATA".equals(type)) {
                JsonArray leaderboardArray = obj.getAsJsonArray("data");
                Type leaderboardListType = new TypeToken<List<client.data.UserProfile>>() {}.getType();
                List<client.data.UserProfile> leaderboardData = gson.fromJson(leaderboardArray, leaderboardListType);

                Platform.runLater(() -> {
                    LeaderboardScene scene = Main.getInstance().getActiveLeaderboardScene();
                    if (scene != null) {
                        scene.updateLeaderboard(leaderboardData);
                        System.out.println("🏆 Đã cập nhật bảng xếp hạng (" + leaderboardData.size() + " người chơi)");
                    }
                });
            }
            else {
                System.out.println("Bỏ qua JSON không thuộc HISTORY_DATA hoặc LEADERBOARD_DATA: " + type);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Lỗi parse JSON từ server: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void listenToServerUDP() {
        try {
            byte[] buffer = new byte[1024];
            while (!udpSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.printf("client.java: %s", message);
                String[] parts = message.split(";");

                // Cập nhật để nhận cả x và y
                if (parts.length == 4 && parts[0].equals("UPDATE_POS")) {
                    System.out.printf("cập nhật vị trí (client.java): %s", message);
                    String senderUsername = parts[1];
                    double xPos = Double.parseDouble(parts[2]);
                    double yPos = Double.parseDouble(parts[3]);

                    if (getUsername() != null && !senderUsername.equals(getUsername())) {
                        Platform.runLater(() -> {
                            GameScene game = Main.getInstance().getActiveGameScene();
                            if (game != null) {
                                game.updateOpponentPosition(senderUsername, xPos, yPos);
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Luồng lắng nghe UDP đã dừng.");
        }
    }

    private void handleServerMessage(String[] messageParts) {
        GameScene game = Main.getInstance().getActiveGameScene();
        System.out.println("check_handleServerMesssage: " + Arrays.toString(messageParts));
        switch (messageParts[0]) {
            case "LOGIN_SUCCESS":
                if (messageParts.length < 2) {
                    Main.getInstance().getLoginScene().showError("Phản hồi đăng nhập không hợp lệ.");
                    return;
                }
                try {
                    String jsonData = messageParts[1];
                    Gson gson = new Gson();
                    // Chuyển chuỗi JSON thành đối tượng UserProfile
                    this.userProfile = gson.fromJson(jsonData, UserProfile.class);

                    if (this.userProfile != null && this.userProfile.getUsername() != null) {
                        System.out.println("Đăng nhập thành công! Chào mừng " + this.userProfile.getUsername());
                        System.out.println("Thông tin người dùng: " + this.userProfile.toString());
                        Main.getInstance().showMenuScene();
                    } else {
                        Main.getInstance().getLoginScene().showError("Dữ liệu người dùng từ server không hợp lệ.");
                    }
                } catch (JsonSyntaxException e) {
                    // Xử lý lỗi nếu server gửi về một chuỗi JSON không đúng định dạng
                    System.err.println("Lỗi phân tích JSON từ server: " + e.getMessage());
                    Main.getInstance().getLoginScene().showError("Lỗi dữ liệu từ server.");
                }
                break;

            case "LOGIN_FAILED":
                String errorMessage = (messageParts.length > 1) ? messageParts[1] : "Tên đăng nhập hoặc mật khẩu không đúng.";
                Main.getInstance().getLoginScene().showError(errorMessage);
                break;

            case "START_GAME":
                if (messageParts.length == 3) {
                    // 2 player
                    String player1Name = messageParts[1];
                    String player2Name = messageParts[2];
                    String myUsername = Client.getInstance().getUsername();
                    String localPlayerName;
                    String remotePlayerName;

                    if (myUsername.equals(player1Name)) {
                        // Mình là player1
                        localPlayerName = player1Name;
                        remotePlayerName = player2Name;
                    } else if (myUsername.equals(player2Name)) {
                        // Mình là player2
                        localPlayerName = player2Name;
                        remotePlayerName = player1Name;
                    } else {
                        // Trường hợp không khớp username (nên log để debug)
                        System.out.println("START_GAME: username client không khớp với serverMessage");
                        localPlayerName = myUsername;
                        remotePlayerName = null;
                    }
                    // Gọi GameScene với vị trí chính xác
                    Main.getInstance().showGameScene(2, localPlayerName, remotePlayerName);
                } else if (messageParts.length == 2) {
                    // 1 player
                    String player1Name = messageParts[1];
                    System.out.println("check_playerName-client: " + player1Name);
                    Main.getInstance().showGameScene(1, player1Name, null);
                } else {
                    System.out.println("START_GAME: messageParts không hợp lệ: " + Arrays.toString(messageParts));
                }
                break;

            case "TIMER_UPDATE": // Server: TIMER_UPDATE;số_giây_còn_lại
                if (game != null && messageParts.length == 2) {
                    int secondsLeft = Integer.parseInt(messageParts[1]);
                    game.updateTimer(secondsLeft);
                }
                break;

            case "SCORE_UPDATE": // Server: SCORE_UPDATE;user1;score1;[user2;score2]
                if (game != null) {
                    try {
                        if (messageParts.length >= 3) {
                            String p1Name = messageParts[1];
                            int p1Score = Integer.parseInt(messageParts[2]);
                            game.updatePlayerScore(p1Name, p1Score);
                        }

                        if (messageParts.length >= 5) {
                            String p2Name = messageParts[3];
                            int p2Score = Integer.parseInt(messageParts[4]);
                            game.updatePlayerScore(p2Name, p2Score);
                        }
                    } catch (Exception e) {
                        System.out.println("Lỗi khi xử lý SCORE_UPDATE: " + e.getMessage());
                    }
                }
                break;

            case "TRASH_SPAWN": // Server: TRASH_SPAWN;id;x;y;type
                if (game != null && messageParts.length == 5) {
                    int id = Integer.parseInt(messageParts[1]);
                    double x = Double.parseDouble(messageParts[2]);
                    double y = Double.parseDouble(messageParts[3]);
                    TrashType type = TrashType.valueOf(messageParts[4]);
                    game.spawnTrash(id, x, y, type);
                }
                break;

            case "TRASH_PICKED_UP": // Server: TRASH_PICKED_UP;username;trashId
                if (game != null && messageParts.length == 3) {
                    String playerName = messageParts[1];
                    int trashId = Integer.parseInt(messageParts[2]);
                    Player player = game.getPlayerByName(playerName);
                    Trash trash = game.getTrashById(trashId);
                    if (player != null && trash != null) {
                        player.pickUpTrash(trash);
                    }
                }
                break;

            case "TRASH_DROPPED": // Server: TRASH_DROPPED;username;trashId
                if (game != null && messageParts.length == 3) {
                    String playerName = messageParts[1];
                    Player player = game.getPlayerByName(playerName);
                    if (player != null) {
                        player.dropTrash();
                    }
                }
                break;
            case "WRONG_CLASSIFY":
                if (game != null && messageParts.length == 2) {
                    String playerName = messageParts[1];
                    game.playerLosesLife(playerName);
                    System.out.println(playerName + " đã phân loại sai và bị trừ điểm!");
                }
                break;

            case "TRASH_RESET": // Server: TRASH_RESET;id;newX;newY;newType
                if (game != null && messageParts.length == 5) {
                    int id = Integer.parseInt(messageParts[1]);
                    double x = Double.parseDouble(messageParts[2]);
                    double y = Double.parseDouble(messageParts[3]);
                    TrashType type = TrashType.valueOf(messageParts[4]);
                    Trash trash = game.getTrashById(id);
                    if (trash != null) {
                        trash.updateState(x, y, type);
                    }
                }
                break;

            case "PAUSE_GAME":
                if (game != null && messageParts.length == 4) {
                    String timeLeft = messageParts[1];
                    String chanceLeft = messageParts[2];
                    String pauserUsername = messageParts[3];
                    // Gọi hàm trong GameScene để hiển thị giao diện tạm dừng
                    game.handleGamePaused(pauserUsername, timeLeft, chanceLeft);
                }
                break;

            // MỚI (Khuyến nghị): Xử lý khi game được tiếp tục
            case "GAME_RESUMED":
                if (game != null) {
                    game.handleGameResumed();
                }
                break;

            case "GAME_OVER":
                if (game != null) {
                    // GAME_OVER có nhiều dạng, nên cần phân tích theo số lượng phần tử
                    if (messageParts.length == 2) {
                        // Trường hợp 1P: GAME_OVER;player1
                        String winner = messageParts[1];
                        game.showGameOver(winner);

                    } else if (messageParts.length == 3 && "DRAW".equals(messageParts[1])) {
                        // Trường hợp hòa: GAME_OVER;DRAW;score1
                        game.showGameOver("TIE");

                    } else if (messageParts.length >= 5) {
                        // Trường hợp 2P: GAME_OVER;username;WIN;score1;score2
                        String playerName = messageParts[1];
                        String resultType = messageParts[2];
                        if ("WIN".equalsIgnoreCase(resultType)) {
                            game.showGameOver(playerName);
                        }
                    } else {
                        System.out.println("CLIENT: Định dạng GAME_OVER không hợp lệ!");
                    }
                }
                break;
            case "LEADERBOARD_DATA":
                if (messageParts.length < 2) {
                    System.err.println("Dữ liệu leaderboard không hợp lệ: thiếu nội dung JSON.");
                    return;
                }
                try {
                    String jsonData = messageParts[1];
                    Gson gson = new Gson();

                    JsonObject responseJson = gson.fromJson(jsonData, JsonObject.class);

                    JsonArray leaderJsonArray = responseJson.getAsJsonArray("data");

                    // 1. Định nghĩa kiểu dữ liệu là một List<UserProfile>
                    Type userProfileListType = new TypeToken<List<UserProfile>>() {}.getType();

                    // 2. Phân tích chuỗi JSON thành một danh sách các UserProfile
                    List<UserProfile> leaderboardData = gson.fromJson(leaderJsonArray, userProfileListType);

                    System.out.println("Parsed HISTORY_DATA count: " + leaderboardData.size());
                    for (client.data.UserProfile userP : leaderboardData) {
                        System.out.println("Leaderboard: " + userP.getUsername() + ", " + userP.getScore());
                    }

                    // 3. Cập nhật giao diện trên luồng chính của JavaFX
                    Platform.runLater(() -> {
                        // Lấy ra LeaderboardScene đang hoạt động (cần có getter trong Main.java)
                        LeaderboardScene scene = Main.getInstance().getActiveLeaderboardScene();
                        if (scene != null) {
                            // Gọi phương thức để cập nhật bảng với dữ liệu mới
                            scene.updateLeaderboard(leaderboardData);
                        } else {
                            System.err.println("Không tìm thấy LeaderboardScene đang hoạt động để cập nhật.");
                        }
                    });

                } catch (JsonSyntaxException e) {
                    System.err.println("Lỗi phân tích JSON lịch sử đấu từ server: " + e.getMessage());
                } catch (Exception e) { // Bắt các lỗi khác có thể xảy ra
                    System.err.println("Lỗi tổng quát khi xử lý LEADERBOARD_DATA: " + e.getMessage());
                    e.printStackTrace();
                }
                break;

            case "LEADERBOARD_FAILED":
                // Xử lý khi server không lấy được dữ liệu leaderboard
                System.err.println("Server không thể lấy dữ liệu leaderboard: " + messageParts[1]);
                // Có thể hiển thị thông báo lỗi trên UI
                break;
            // client.network.Client.java

// ... (bên trong handleServerMessage, case "HISTORY_DATA") ...

            case "HISTORY_DATA":
                if (messageParts.length < 2) {
                    System.err.println("Dữ liệu lịch sử đấu không hợp lệ.");
                    return;
                }
                try {
                    String fullJsonString = messageParts[1]; // Đây là toàn bộ phần JSON sau "HISTORY_DATA;"
                    Gson gson = new Gson();

                    // Bước 1: Parse toàn bộ chuỗi JSON thành một JsonObject
                    JsonObject responseJson = gson.fromJson(fullJsonString, JsonObject.class);

                    // Bước 2: Lấy phần "data" từ JsonObject. "data" sẽ là một JsonArray
                    JsonArray historyJsonArray = responseJson.getAsJsonArray("data");

                    // Bước 3: Định nghĩa kiểu dữ liệu là một List<MatchHistory>
                    Type matchHistoryListType = new TypeToken<List<client.data.MatchHistory>>() {}.getType();

                    // Bước 4: Phân tích JsonArray thành danh sách đối tượng
                    List<client.data.MatchHistory> historyData = gson.fromJson(historyJsonArray, matchHistoryListType);

                    System.out.println("Parsed HISTORY_DATA count: " + historyData.size());
                    for (client.data.MatchHistory match : historyData) {
                        System.out.println("Match: " + match.getOpponentName() + ", " + match.getResult() + ", " + match.getStartDate() + ", " + match.getGameDate());
                    }

                    // Cập nhật giao diện trên luồng chính của JavaFX
                    Platform.runLater(() -> {
                        HistoryScene scene = Main.getInstance().getActiveHistoryScene();
                        if (scene != null) {
                            scene.updateHistory(historyData);
                        } else {
                            System.err.println("Không tìm thấy HistoryScene đang hoạt động để cập nhật.");
                        }
                    });

                } catch (JsonSyntaxException e) {
                    System.err.println("Lỗi phân tích JSON lịch sử đấu từ server: " + e.getMessage());
                } catch (Exception e) { // Bắt các lỗi khác có thể xảy ra
                    System.err.println("Lỗi tổng quát khi xử lý HISTORY_DATA: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
// ...

            case "HISTORY_FAILED":
                System.err.println("Server không thể lấy dữ liệu lịch sử đấu: " + messageParts[1]);
                // Có thể hiển thị thông báo lỗi trên UI
                break;
        }
    }
    // MỚI: Hàm để gửi yêu cầu tạm dừng game đến server
    public void requestPauseGame() {
        // Server đã biết bạn là ai thông qua kết nối TCP,
        // bạn chỉ cần gửi lệnh.
        sendMessage("PAUSE_GAME");
    }

    // MỚI (Khuyến nghị): Hàm để gửi yêu cầu tiếp tục game
    public void requestResumeGame() {
        sendMessage("RESUME_GAME");
    }

    public void sendMessage(String message) {
        tcpOut.println(message);
    }

    public void sendUDPMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, udpPort);
            udpSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return (this.userProfile != null) ? this.userProfile.getUsername() : null;
    }

    public void setUsername(String username) {this.userProfile.setUsername(username);}

    public int getHighScore() {
        return (this.userProfile != null) ? this.userProfile.getScore() : 0;
    }


    public UserProfile getUserProfile() {
        return this.userProfile;
    }

    public void requestMatchHistory() {
        // Gửi lệnh GET_HISTORY. Server của bạn dùng this.username, nên không cần gửi kèm username
        sendMessage("GET_HISTORY");
        System.out.println("DEBUG (Client): Đã gửi lệnh GET_HISTORY tới server.");
    }

    public void requestLeaderboard() {
        // Gửi lệnh GET_LEADERBOARD. Server của bạn dùng this.username, nên không cần gửi kèm username
        sendMessage("GET_LEADERBOARD");
        System.out.println("DEBUG (Client): Đã gửi lệnh GET_LEADERBOARD tới server.");
    }


    public void close() {
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
