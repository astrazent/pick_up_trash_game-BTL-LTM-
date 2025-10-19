package gamePlay.network;

import gamePlay.config.NetworkConfig;
import gamePlay.utils.ResourceLoader;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private final int tcpPort;
    private final int udpPort;

    private final Set<ClientTCPHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private final List<ClientTCPHandler> waitingPlayers = Collections.synchronizedList(new ArrayList<>());
    private final List<GameRoom> activeRooms = Collections.synchronizedList(new ArrayList<>());
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
                System.out.println("check_startUDP: " + message);
                if (parts.length < 2) continue;
                String username = parts[1];

                // --- Lưu địa chỉ UDP của client ---
                InetSocketAddress clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
                ClientTCPHandler handler = findHandlerByUsername(username);
                if (handler != null) {
                    handler.setUdpAddress(clientAddress);
                }
                // --- Tìm phòng mà người chơi này đang ở ---
                GameRoom room = findRoomByPlayer(username);
                if (room != null) {
                    room.broadcastUDP(message, username);
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi UDP Server: " + e.getMessage());
        }
    }

    public void sendUDPMessage(String message, InetSocketAddress targetAddress) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress.getAddress(), targetAddress.getPort());
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.send(packet);
            }
        } catch (IOException e) {
            System.err.println("Lỗi gửi gói tin UDP tới " + targetAddress);
        }
    }

    public synchronized void playerIsReady(ClientTCPHandler readyPlayer, int playerCount) {
        // TRƯỜNG HỢP 1: Chế độ một người chơi
        if (playerCount == 1) {
            System.out.println("Yêu cầu chơi đơn từ: " + readyPlayer.getUsername() + ". Tạo phòng game mới.");
            GameRoom newRoom = new GameRoom(readyPlayer, this); // Sử dụng constructor mới của GameRoom
            readyPlayer.setCurrentRoom(newRoom);
            activeRooms.add(newRoom);
            new Thread(newRoom).start();
            return; // Kết thúc, không cần thêm vào hàng đợi
        }

        // TRƯỜNG HỢP 2: Chế độ hai người chơi (giữ nguyên logic cũ)
        if (!waitingPlayers.contains(readyPlayer)) {
            waitingPlayers.add(readyPlayer);
            System.out.println("check_handle-player-is-ready:" + System.identityHashCode(readyPlayer));
            System.out.println("Player is ready: " + readyPlayer.getUsername() + ". Total waiting: " + waitingPlayers.size());
        }

        if (waitingPlayers.size() >= 2) {
            System.out.println("Hai người chơi đã sẵn sàng! Tạo phòng game mới.");
            ClientTCPHandler player1 = waitingPlayers.remove(0);
            ClientTCPHandler player2 = waitingPlayers.remove(0);
            GameRoom newRoom = new GameRoom(player1, player2, this);
            player1.setCurrentRoom(newRoom);
            player2.setCurrentRoom(newRoom);
            System.out.println("check room_player-is-ready: " + System.identityHashCode(newRoom));
            activeRooms.add(newRoom);
            new Thread(newRoom).start();
        }
    }

    public void removeClient(ClientTCPHandler handler) {
        handler.cleanup();
        clientHandlers.remove(handler);
        // Chỉ xóa phòng nếu không còn ai
        if (clientHandlers.isEmpty()) {
            activeRooms.remove(handler.getCurrentRoom());
            System.out.println("Đã xóa phòng " + handler.getCurrentRoom().getRoomCode() + " vì không còn người chơi.");
        }
        System.out.println("Một client đã ngắt kết nối. Còn lại: " + clientHandlers.size());
    }

    // --- Tìm phòng chứa người chơi ---
    public GameRoom findRoomByPlayer(String username) {
        for (GameRoom room : activeRooms) {
            if (room.getPlayer1().getUsername().equals(username) ||
                    room.getPlayer2().getUsername().equals(username)) {
                return room;
            }
        }
        return null;
    }

    // --- Tìm handler theo username ---
    public ClientTCPHandler findHandlerByUsername(String username) {
        synchronized (clientHandlers) {
            for (ClientTCPHandler handler : clientHandlers) {
                if (handler.getUsername() != null && handler.getUsername().equals(username)) {
                    return handler;
                }
            }
        }
        return null;
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

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
