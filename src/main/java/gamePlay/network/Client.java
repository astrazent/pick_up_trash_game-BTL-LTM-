package gamePlay.network;

import gamePlay.Main;
import gamePlay.config.NetworkConfig;
import gamePlay.scenes.GameScene;
import javafx.application.Platform;
import javafx.scene.Scene;

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
        udpSocket = new DatagramSocket(); // Cổng UDP client sẽ được HĐH chọn
        tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
        tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

        // Bắt đầu cả 2 luồng lắng nghe
        new Thread(this::listenToServerTCP).start();
        new Thread(this::listenToServerUDP).start();
    }

    // --- Luồng lắng nghe TCP ---
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

    // --- Luồng lắng nghe UDP ---
    private void listenToServerUDP() {
        try {
            byte[] buffer = new byte[1024];
            while (!udpSocket.isClosed()) { // Dùng isClosed() để vòng lặp có thể thoát
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String[] parts = message.split(";");

                if (parts.length > 0 && parts[0].equals("UPDATE_POS")) {
                    String senderUsername = parts[1];
                    double xPos = Double.parseDouble(parts[2]);

                    if (this.username != null && !senderUsername.equals(this.username)) {
                        Platform.runLater(() -> {
                            // --- LOGIC MỚI, ĐÚNG ĐẮN ---
                            Main mainApp = Main.getInstance();
                            GameScene game = mainApp.getActiveGameScene();

                            // Chỉ cập nhật nếu chúng ta thực sự đang ở trong màn hình game
                            if (game != null) {
                                game.updateOpponentPosition(senderUsername, xPos);
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Luồng lắng nghe UDP đã dừng.");
        }
    }

    // --- Xử lý các thông điệp TCP ---
    private void handleServerMessage(String[] messageParts) {
        switch (messageParts[0]) {
            case "LOGIN_SUCCESS":
                this.username = messageParts[1];
                Main.getInstance().showMenuScene();
                break;

            case "LOGIN_FAILED":
                Main.getInstance().getLoginScene().showError(messageParts[1]);
                break;

            case "START_GAME":
                // Server gửi: START_GAME;player1;player2
                if (messageParts.length == 3) {
                    String player1Name = messageParts[1];
                    String player2Name = messageParts[2];
                    Main.getInstance().showGameScene(2, player1Name, player2Name);
                }
                break;
        }
    }

    // --- Gửi dữ liệu TCP ---
    public void sendMessage(String message) {
        tcpOut.println(message);
    }

    // --- Gửi dữ liệu UDP ---
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

    // --- Đóng kết nối ---
    public void close() {
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
