package gamePlay.network;

import gamePlay.Main;
import gamePlay.config.NetworkConfig;
import gamePlay.game.Player;
import gamePlay.game.Trash;
import gamePlay.game.TrashType;
import gamePlay.scenes.GameScene;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class Client {
    private static Client instance;
    private final String host;
    private final int tcpPort;
    private final int udpPort;

    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private PrintWriter tcpOut;
    private BufferedReader tcpIn;

    private String username;

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
                String[] parts = serverMessage.split(";");
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
                String[] parts = message.split(";");

                // Cập nhật để nhận cả x và y
                if (parts.length == 4 && parts[0].equals("UPDATE_POS")) {
                    String senderUsername = parts[1];
                    double xPos = Double.parseDouble(parts[2]);
                    double yPos = Double.parseDouble(parts[3]);

                    if (this.username != null && !senderUsername.equals(this.username)) {
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

        switch (messageParts[0]) {
            case "LOGIN_SUCCESS":
                this.username = messageParts[1];
                Main.getInstance().showMenuScene();
                break;

            case "LOGIN_FAILED":
                Main.getInstance().getLoginScene().showError(messageParts[1]);
                break;

            case "START_GAME":
                if (messageParts.length == 3) {
                    String player1Name = messageParts[1];
                    String player2Name = messageParts[2];
                    Main.getInstance().showGameScene(2, player1Name, player2Name);
                }
                break;

            case "TIMER_UPDATE": // Server: TIMER_UPDATE;số_giây_còn_lại
                if (game != null && messageParts.length == 2) {
                    int secondsLeft = Integer.parseInt(messageParts[1]);
                    game.updateTimer(secondsLeft);
                }
                break;

            case "SCORE_UPDATE": // Server: SCORE_UPDATE;user1;score1;user2;score2
                if (game != null && messageParts.length == 5) {
                    String p1Name = messageParts[1];
                    int p1Score = Integer.parseInt(messageParts[2]);
                    String p2Name = messageParts[3];
                    int p2Score = Integer.parseInt(messageParts[4]);
                    game.updatePlayerScore(p1Name, p1Score);
                    game.updatePlayerScore(p2Name, p2Score);
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

            case "GAME_OVER": // Server: GAME_OVER;winnerName;message
                if (game != null && messageParts.length >= 2) {
                    String winner = messageParts[1];
                    game.showGameOver(winner);
                }
                break;
        }
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
        return username;
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
