package gamePlay.network;

import gamePlay.config.NetworkConfig;
import gamePlay.utils.ResourceLoader;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private final int tcpPort;
    private final int udpPort;

    // Danh sách handler của các client TCP đang kết nối
    private final Set<ClientTCPHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    // Lưu thông tin client UDP (username -> địa chỉ)
    private final ConcurrentHashMap<String, InetSocketAddress> connectedClients = new ConcurrentHashMap<>();

    // Danh sách người chơi sẵn sàng
    private final Set<String> readyPlayers = Collections.synchronizedSet(new HashSet<>());

    private DatagramSocket udpSocket;

    public GameServer(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
    }

    public void start() {
        System.out.println("Server đang khởi động...");
        new Thread(this::startTCP).start();
        new Thread(this::startUDP).start();
        System.out.println("Server đã sẵn sàng!");
    }

    // --- Xử lý TCP ---
    private void startTCP() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("TCP Server đang lắng nghe trên cổng: " + tcpPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientTCPHandler handler = new ClientTCPHandler(clientSocket, this);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi TCP Server: " + e.getMessage());
        }
    }

    // --- Xử lý UDP ---
    private void startUDP() {
        try {
            udpSocket = new DatagramSocket(udpPort);
            System.out.println("UDP Server đang lắng nghe trên cổng: " + udpPort);
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                String[] parts = message.split(";");
                if (parts.length < 2) continue;
                String username = parts[1];

                connectedClients.put(username, (InetSocketAddress) packet.getSocketAddress());

                broadcastUDP(message, username);
            }
        } catch (IOException e) {
            System.err.println("Lỗi UDP Server: " + e.getMessage());
        }
    }

    // --- Gửi UDP tới tất cả client trừ người gửi ---
    public void broadcastUDP(String message, String senderUsername) {
        byte[] data = message.getBytes();
        for (String username : connectedClients.keySet()) {
            if (!username.equals(senderUsername)) {
                try {
                    InetSocketAddress address = connectedClients.get(username);
                    DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());
                    udpSocket.send(packet);
                } catch (IOException e) {
                    System.err.println("Lỗi gửi gói tin UDP tới " + username);
                }
            }
        }
    }

    // --- Khi người chơi bấm "Sẵn sàng" ---
    public synchronized void playerIsReady(String username) {
        readyPlayers.add(username);
        System.out.println("Player is ready: " + username + ". Total ready: " + readyPlayers.size());

        if (readyPlayers.size() == 2) {
            System.out.println("Hai người chơi đã sẵn sàng! Gửi tín hiệu bắt đầu game.");
            String players = String.join(";", readyPlayers);
            broadcastTCP("START_GAME;" + players);
            readyPlayers.clear();
        }
    }

    // --- Gửi tin nhắn TCP tới tất cả client ---
    public void broadcastTCP(String message) {
        synchronized (clientHandlers) {
            for (ClientTCPHandler handler : clientHandlers) {
                handler.sendMessage(message);
            }
        }
    }

    // --- Xóa client khi ngắt kết nối ---
    public void removeClient(ClientTCPHandler handler) {
        clientHandlers.remove(handler);
        System.out.println("Một client đã ngắt kết nối. Còn lại: " + clientHandlers.size());
    }

    // --- Khởi động server ---
    public static void main(String[] args) {
        NetworkConfig config = ResourceLoader.loadNetworkConfig();
        if (config != null) {
            GameServer server = new GameServer(config.server.tcp_port, config.server.udp_port);
            server.start();
        } else {
            System.err.println("Không thể khởi động server do lỗi cấu hình.");
        }
    }
}
