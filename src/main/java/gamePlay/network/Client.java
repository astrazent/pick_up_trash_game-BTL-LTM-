package gamePlay.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import gamePlay.Main;
import gamePlay.config.NetworkConfig;
import gamePlay.data.MatchHistory;
import gamePlay.data.UserProfile;
import gamePlay.game.Player;
import gamePlay.game.Trash;
import gamePlay.game.TrashType;
import gamePlay.scenes.GameScene;
import gamePlay.scenes.HistoryScene;
import gamePlay.scenes.LeaderboardScene;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Type;

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
                final String[] parts = serverMessage.split(";");
                Platform.runLater(() -> handleServerMessage(parts));
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối TCP với server.");
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

                    // 1. Định nghĩa kiểu dữ liệu là một List<UserProfile>
                    Type userProfileListType = new TypeToken<List<UserProfile>>() {}.getType();

                    // 2. Phân tích chuỗi JSON thành một danh sách các UserProfile
                    List<UserProfile> leaderboardData = gson.fromJson(jsonData, userProfileListType);

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
                    // Xử lý lỗi nếu server gửi về một chuỗi JSON không đúng định dạng
                    System.err.println("Lỗi phân tích JSON từ server: " + e.getMessage());
                    // Có thể hiển thị thông báo lỗi trên UI nếu cần
                }
                break;

            case "LEADERBOARD_FAILED":
                // Xử lý khi server không lấy được dữ liệu leaderboard
                System.err.println("Server không thể lấy dữ liệu leaderboard: " + messageParts[1]);
                // Có thể hiển thị thông báo lỗi trên UI
                break;
            case "HISTORY_DATA":
                if (messageParts.length < 2) {
                    System.err.println("Dữ liệu lịch sử đấu không hợp lệ.");
                    return;
                }
                try {
                    String jsonData = messageParts[1];
                    Gson gson = new Gson();

                    // Định nghĩa kiểu dữ liệu là một List<MatchHistory>
                    Type matchHistoryListType = new TypeToken<List<MatchHistory>>() {}.getType();

                    // Phân tích JSON thành danh sách đối tượng
                    List<MatchHistory> historyData = gson.fromJson(jsonData, matchHistoryListType);

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
                }
                break;

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

    public UserProfile getUserProfile() {
        return this.userProfile;
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
