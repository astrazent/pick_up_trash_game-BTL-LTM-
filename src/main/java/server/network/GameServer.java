package server.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import server.config.NetworkConfig;
import server.utils.ResourceLoader;

public class GameServer {
    private final int tcpPort;
    private final int udpPort;

    private final Set<server.network.ClientTCPHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private final List<server.network.ClientTCPHandler> waitingPlayers = Collections.synchronizedList(new ArrayList<>());
    private final List<server.network.GameRoom> activeRooms = Collections.synchronizedList(new ArrayList<>());
    private DatagramSocket udpSocket;

    public GameServer(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
    }

    public void start() {
        System.out.println("Server dang khoi dong...");
        new Thread(this::startTCP).start();
        new Thread(this::startUDP).start();
        System.out.println("Server da san sang!");
    }

    private void startTCP() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("TCP Server dang lang nghe tren cong " + tcpPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                server.network.ClientTCPHandler handler = new server.network.ClientTCPHandler(clientSocket, this);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Loi TCP Server: " + e.getMessage());
        }
    }

    private void startUDP() {
        try {
            udpSocket = new DatagramSocket(udpPort);
            System.out.println("UDP Server dang lang nghe tren cong " + udpPort);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String[] parts = message.split(";");
                if (parts.length < 2) continue;
                String username = parts[1];

                // --- Lưu địa chỉ UDP của client ---
                InetSocketAddress clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
                server.network.ClientTCPHandler handler = findHandlerByUsername(username);
                if (handler != null) {
                    handler.setUdpAddress(clientAddress);
                }
                // --- Tìm phòng mà người chơi này đang ở ---
                server.network.GameRoom room = findRoomByPlayer(username);
                if (room != null) {
                    room.broadcastUDP(message, username);
                }
            }
        } catch (IOException e) {
            System.err.println("Loi UDP Server: " + e.getMessage());
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
            System.err.println("Loi gui goi tin UDP toi " + targetAddress);
        }
    }

    public synchronized void playerIsReady(server.network.ClientTCPHandler readyPlayer, int playerCount) {
        // TRƯỜNG HỢP 1: Chế độ một người chơi
        if (playerCount == 1) {
            System.out.println("Yeu cau choi den tu: " + readyPlayer.getUsername() + ". Tao phong game moi.");
            server.network.GameRoom newRoom = new server.network.GameRoom(readyPlayer, this); // Sử dụng constructor mới của GameRoom
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
            System.out.println("Hai nguoi choi da san sang! Tao phong game moi.");
            server.network.ClientTCPHandler player1 = waitingPlayers.remove(0);
            server.network.ClientTCPHandler player2 = waitingPlayers.remove(0);
            server.network.GameRoom newRoom = new server.network.GameRoom(player1, player2, this);
            player1.setCurrentRoom(newRoom);
            player2.setCurrentRoom(newRoom);
            System.out.println("check room_player-is-ready: " + System.identityHashCode(newRoom));
            activeRooms.add(newRoom);
            new Thread(newRoom).start();
        }
    }

    public synchronized void challengeAccepted(server.network.ClientTCPHandler senderPlayer, server.network.ClientTCPHandler receiverPlayer) {
        if (clientHandlers.contains(senderPlayer) && clientHandlers.contains(receiverPlayer)) {
            System.out.println("Tao phong game moi.");
            server.network.GameRoom newRoom = new server.network.GameRoom(senderPlayer, receiverPlayer, this);
            senderPlayer.setCurrentRoom(newRoom);
            receiverPlayer.setCurrentRoom(newRoom);
            System.out.println("check room_player-is-ready: " + System.identityHashCode(newRoom));
            activeRooms.add(newRoom);
            new Thread(newRoom).start();
        }
        else{
            System.out.println("Doi thu khong the vao phong!");
        }
    }

    public synchronized void sendChallengeRequest(ClientTCPHandler opponentHandler, String senderUsername) {
        ClientTCPHandler challengerHandler = findHandlerByUsername(senderUsername);
        String opponent=opponentHandler.getUsername();
        if (challengerHandler == null) {
            return;
        }

        // Nếu đối thủ đang chờ phản hồi một thách đấu khác
        if (opponentHandler.isWaiting()) {
            challengerHandler.setWaiting(false);
            challengerHandler.setOpponentName(null);
            challengerHandler.sendMessage("AUTO_DECLINE;" + opponent);
            return;
        }

        if (opponentHandler != null) {
            opponentHandler.setWaiting(true);
            opponentHandler.setChallengerName(senderUsername);
            opponentHandler.sendMessage("RECEIVE_CHALLENGE;" + senderUsername);
        }
    }

    public synchronized void challengeDeclined(server.network.ClientTCPHandler senderPlayer) {
        if (clientHandlers.contains(senderPlayer)) {
            senderPlayer.setWaiting(false);
            senderPlayer.setOpponentName(null);
            senderPlayer.sendMessage("CHALLENGE_DECLINED");
        }
        else{
            System.out.println("Thong diep Decline ko hop le");
        }
    }

    public synchronized void cancelWaiting(server.network.ClientTCPHandler player) {
        if (waitingPlayers.remove(player)) {
            System.out.println("Player " + player.getUsername() + " da huy cho doi. Con lai: " + waitingPlayers.size());
            player.sendMessage("CANCEL_WAITING_SUCCESS");
        }
    }

    public void removeClient(server.network.ClientTCPHandler handler) {
        if (handler.isWaiting()){
            if (handler.getChallengerName()!=null){
                String challengerName=handler.getChallengerName();
                ClientTCPHandler challengerHandler = findHandlerByUsername(challengerName);
                challengerHandler.setWaiting(false);
                challengerHandler.setOpponentName(null);
                challengerHandler.sendMessage("OPPONENT_OFFLINE");
            }
            if (handler.getOpponentName()!=null){
                String opponentName=handler.getOpponentName();
                ClientTCPHandler opponentHandler = findHandlerByUsername(opponentName);
                opponentHandler.setWaiting(false);
                opponentHandler.setChallengerName(null);
            }
        }

        // Dọn tài nguyên socket
        handler.cleanup();

        // Nếu người chơi đang ở trong phòng, xóa họ khỏi phòng
        GameRoom room = handler.getCurrentRoom();
        if (room != null) {
            String leavingClientName = handler.getUsername();
            room.removePlayer(leavingClientName); // Bạn cần có hàm removePlayer trong GameRoom
        }

        // Xóa client khỏi danh sách đang hoạt động
        clientHandlers.remove(handler);

        if (room != null) {
            activeRooms.remove(room);
        }
    }

    // --- Tìm phòng chứa người chơi ---
    public server.network.GameRoom findRoomByPlayer(String username) {
        for (GameRoom room : activeRooms) {
            if (room.getPlayer1().getUsername().equals(username) ||
                    room.getPlayer2().getUsername().equals(username)) {
                return room;
            }
        }
        return null;
    }

    // --- Tìm handler theo username ---
    public server.network.ClientTCPHandler findHandlerByUsername(String username) {
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

    public synchronized void broadcastOnlineList() {
        StringBuilder sb = new StringBuilder("ONLINE_LIST");
        for (ClientTCPHandler handler : clientHandlers) {
            String username = handler.getUsername();
            if (username != null) {
                sb.append(";").append(username);
            }
        }
        String message = sb.toString();

        for (ClientTCPHandler handler : clientHandlers) {
            handler.sendMessage(message);
        }
    }

    public synchronized void changeClientUsername(ClientTCPHandler handler, String newUsername) {
        if (handler != null) {
            handler.setUsername(newUsername);
            broadcastOnlineList();
        }
    }

    public void removeRoom(GameRoom room) {
        activeRooms.remove(room);
    }

    public boolean isOnline(ClientTCPHandler handler) {
        return clientHandlers.contains(handler);
    }

    // Luồng tự động hiển thị online mỗi 10 giây
//    private void broadcastOnlineList() {
//        while (true) {
//            try {
//                Thread.sleep(10000); // 10 giây
//                broadcastOnlineList();
//            } catch (InterruptedException e) {
//                break;
//            }
//        }
//    }

    public static void main(String[] args) {
        NetworkConfig config = ResourceLoader.loadNetworkConfig();

        if (config != null) {
            GameServer server = new GameServer(config.server.tcp_port, config.server.udp_port);
            server.start();
        } else {
            System.err.println("Khong the khoi dong server do loi cau hinh.");
        }
    }
}
