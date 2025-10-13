package gamePlay.network;

import gamePlay.game.TrashType;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class TrashState {
    public int id;
    public double x, y;
    public TrashType type;
    public String heldBy = null;

    public TrashState(int id, double x, double y, TrashType type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
    }
}

public class GameRoom implements Runnable {
    private final ClientTCPHandler player1;
    private final ClientTCPHandler player2;
    private final GameServer server;

    private boolean isRunning = false;
    private final int gameDurationSeconds = 120;
    private final long trashSpawnIntervalMs = 5000;

    private static final AtomicInteger trashIdCounter = new AtomicInteger(0);

    private final Map<Integer, TrashState> trashStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();

    public GameRoom(ClientTCPHandler p1, ClientTCPHandler p2, GameServer server) {
        this.player1 = p1;
        this.player2 = p2;
        this.server = server;
        playerScores.put(p1.getUsername(), 0);
        playerScores.put(p2.getUsername(), 0);
    }

    @Override
    public void run() {
        try {
            isRunning = true;
            System.out.println("SERVER: GameRoom bắt đầu giữa " + player1.getUsername() + " và " + player2.getUsername());
            broadcast("START_GAME;" + player1.getUsername() + ";" + player2.getUsername());

            int secondsLeft = gameDurationSeconds;
            long lastTrashSpawnTime = System.currentTimeMillis();

            while (isRunning && secondsLeft > 0) {
                Thread.sleep(1000);
                secondsLeft--;

                broadcast("TIMER_UPDATE;" + secondsLeft);

                if (System.currentTimeMillis() - lastTrashSpawnTime > trashSpawnIntervalMs) {
                    spawnNewTrash();
                    lastTrashSpawnTime = System.currentTimeMillis();
                }
            }

            System.out.println("SERVER: [Room " + player1.getUsername() + "] Game Over.");
            broadcast("GAME_OVER;Player1"); // Tạm thời người thắng là Player1

        } catch (InterruptedException e) {
            System.out.println("GameRoom bị gián đoạn.");
            isRunning = false;
        }
    }

    private void spawnNewTrash() {
        int id = trashIdCounter.getAndIncrement();
        TrashType type = TrashType.values()[(int) (Math.random() * TrashType.values().length)];
        double x = Math.random() * 750;
        double y = -50.0;

        trashStates.put(id, new TrashState(id, x, y, type));

        String message = String.format("TRASH_SPAWN;%d;%f;%f;%s", id, x, y, type.name());
        broadcast(message);
    }

    public void handleGameMessage(String message, String senderUsername) {
        String[] parts = message.split(";");
        String command = parts[0];

        switch (command) {
            case "PICK_TRASH":
                handlePickTrash(senderUsername, Integer.parseInt(parts[2]));
                break;
            case "DROP_TRASH":
                handleDropTrash(senderUsername, parts[2]);
                break;
        }
    }

    private void handlePickTrash(String username, int trashId) {
        TrashState trash = trashStates.get(trashId);

        if (trash != null && trash.heldBy == null) {
            boolean alreadyHolding = trashStates.values().stream().anyMatch(t -> username.equals(t.heldBy));
            if (!alreadyHolding) {
                trash.heldBy = username;
                broadcast("TRASH_PICKED_UP;" + username + ";" + trashId);
            }
        }
    }

    private void handleDropTrash(String username, String binTypeName) {
        TrashState heldTrash = null;
        int trashId = -1;

        for (TrashState trash : trashStates.values()) {
            if (username.equals(trash.heldBy)) {
                heldTrash = trash;
                trashId = trash.id;
                break;
            }
        }

        if (heldTrash != null) {
            TrashType binType = TrashType.valueOf(binTypeName);

            if (heldTrash.type == binType) {
                playerScores.compute(username, (k, v) -> v + 1);
            } else {
                playerScores.compute(username, (k, v) -> v - 1);
            }

            broadcast(String.format("SCORE_UPDATE;%s;%d;%s;%d",
                    player1.getUsername(), playerScores.get(player1.getUsername()),
                    player2.getUsername(), playerScores.get(player2.getUsername())
            ));

            broadcast("TRASH_DROPPED;" + username + ";" + trashId);

            heldTrash.heldBy = null;
            heldTrash.x = Math.random() * 750;
            heldTrash.y = -50;
            heldTrash.type = TrashType.values()[(int) (Math.random() * TrashType.values().length)];

            broadcast(String.format("TRASH_RESET;%d;%f;%f;%s",
                    heldTrash.id, heldTrash.x, heldTrash.y, heldTrash.type.name()
            ));
        }
    }

    public void broadcast(String message) {
        player1.sendMessage(message);
        player2.sendMessage(message);
    }
    public void broadcastUDP(String message, String senderUsername) {
        // 1. Xác định người nhận là ai
        ClientTCPHandler receiver;
        if (player1.getUsername().equals(senderUsername)) {
            receiver = player2;
        } else {
            receiver = player1;
        }

        // 2. Lấy địa chỉ UDP của người nhận (đã được lưu trong handler)
        InetSocketAddress receiverAddress = receiver.getUdpAddress();

        // 3. Nếu có địa chỉ hợp lệ, yêu cầu server gửi tin nhắn UDP đến đó
        if (receiverAddress != null) {
            server.sendUDPMessage(message, receiverAddress);
        } else {
            // Dòng này để debug, nếu bạn thấy nó tức là địa chỉ UDP chưa được lưu
            System.out.println("SERVER WARNING: Không tìm thấy địa chỉ UDP cho người nhận " + receiver.getUsername());
        }
    }

    public ClientTCPHandler getPlayer1() { return player1; }
    public ClientTCPHandler getPlayer2() { return player2; }
}
