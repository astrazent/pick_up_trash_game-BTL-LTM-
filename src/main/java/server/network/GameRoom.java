package server.network;

import client.game.TrashType;
import server.network.ClientTCPHandler;
import server.network.GameServer;
import server.utils.DatabaseConnector;
import server.utils.DatabaseResponse;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final server.network.ClientTCPHandler player1;
    public final server.network.ClientTCPHandler player2;
    private final GameServer server;
    private final String roomCode; // Mã phòng (6 ký tự)
    private Thread pauseThread;
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    private final int gameDurationSeconds = 120;
    private final long trashSpawnIntervalMs = 5000;
    private int pauseChancesPlayer1 = 3;
    private int pauseChancesPlayer2 = 3;
    private int alive1P = 3;

    private static final AtomicInteger trashIdCounter = new AtomicInteger(0);

    private final Map<Integer, TrashState> trashStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();

    public GameRoom(server.network.ClientTCPHandler p1, server.network.ClientTCPHandler p2, GameServer server) {
        this.player1 = p1;
        this.player2 = p2;
        System.out.println("GameRoom created. h1: " + System.identityHashCode(p1) + " h2: " + System.identityHashCode(p2));
        this.server = server;
        if (p1 != null) playerScores.put(p1.getUsername(), 0);
        if (p2 != null) playerScores.put(p2.getUsername(), 0);
        this.roomCode = "2P" + generateRoomCode();
    }

    public GameRoom(server.network.ClientTCPHandler p1, GameServer server) {
        this.player1 = p1;
        this.player2 = null;
        this.server = server;
        if (p1 != null) playerScores.put(p1.getUsername(), 0);
        this.roomCode = "1P" + generateRoomCode();
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }

    public String getRoomCode() {
        return roomCode;
    }

    @Override
    public void run() {
        try {
            isRunning = true;
            int matchId1 = -1;
            int matchId2 = -1;

            if (player2 != null) {
                System.out.println("SERVER: GameRoom bắt đầu giữa " + player1.getUsername() + " và " + player2.getUsername());
                broadcast("START_GAME;" + player1.getUsername() + ";" + player2.getUsername());

                // Gọi startMatch cho cả 2 player, retry 1 lần nếu cần
                DatabaseResponse<Integer> res1 = DatabaseConnector.startMatch(player1.getUsername(), player2.getUsername());
                if (!res1.isSuccess()) {
                    System.err.println("WARN: startMatch cho player1 thất bại, thử lại...");
                    res1 = DatabaseConnector.startMatch(player1.getUsername(), player2.getUsername());
                }
                if (res1.isSuccess()) {
                    matchId1 = res1.getData();
                    System.out.println(res1.getMessage());
                } else {
                    System.err.println("❌ startMatch vẫn thất bại cho player1: " + player1.getUsername() + " — " + res1.getMessage());
                }

                DatabaseResponse<Integer> res2 = DatabaseConnector.startMatch(player2.getUsername(), player1.getUsername());
                if (!res2.isSuccess()) {
                    System.err.println("WARN: startMatch cho player2 thất bại, thử lại...");
                    res2 = DatabaseConnector.startMatch(player2.getUsername(), player1.getUsername());
                }
                if (res2.isSuccess()) {
                    matchId2 = res2.getData();
                    System.out.println(res2.getMessage());
                } else {
                    System.err.println("❌ startMatch vẫn thất bại cho player2: " + player2.getUsername() + " — " + res2.getMessage());
                }

                if (!res1.isSuccess() || !res2.isSuccess()) {
                    System.err.println("❌ Lỗi tạo match_id cho player: " +
                            (!res1.isSuccess() ? player1.getUsername() : player2.getUsername()));
                    // Ghi rõ lỗi và kết thúc phòng để không sinh trạng thái không đồng bộ
                    broadcast("GAME_ERROR;CANNOT_START_MATCH");
                    return;
                }

            } else {
                System.out.println("SERVER: GameRoom (1P) bắt đầu cho " + player1.getUsername());
                broadcast("START_GAME;" + player1.getUsername());
                DatabaseResponse<Integer> res = DatabaseConnector.startMatch(player1.getUsername(), player1.getUsername());
                if (res.isSuccess()) {
                    matchId1 = res.getData();
                    System.out.println(res.getMessage());
                } else {
                    System.err.println("❌ startMatch 1P thất bại: " + res.getMessage());
                    broadcast("GAME_ERROR;CANNOT_START_MATCH");
                    return;
                }
            }

            int secondsLeft = gameDurationSeconds;
            long lastTrashSpawnTime = System.currentTimeMillis();

            while (isRunning && secondsLeft > 0) {
                if (!isPaused) {
                    Thread.sleep(1000);
                    secondsLeft--;
                    broadcast("TIMER_UPDATE;" + secondsLeft);

                    if (System.currentTimeMillis() - lastTrashSpawnTime > trashSpawnIntervalMs) {
                        spawnNewTrash();
                        lastTrashSpawnTime = System.currentTimeMillis();
                    }
                } else {
                    Thread.sleep(100);
                }
            }

            // Lấy điểm hiện tại của hai người chơi
            int score1 = playerScores.getOrDefault(player1.getUsername(), 0);
            int score2 = 0;

            if (player2 != null) {
                score2 = playerScores.getOrDefault(player2.getUsername(), 0);
            } else {
                // 1P kết thúc
                System.out.println("SERVER: [Player: " + player1.getUsername() + "] complete 1P match");
                DatabaseResponse<Void> res = DatabaseConnector.updateScoreAfterMatch(matchId1, player1.getUsername(), "win");
                System.out.println(res.getMessage());
                broadcast("GAME_OVER;" + player1.getUsername());
                return;
            }

            String winner;
            if (score1 > score2) {
                winner = player1.getUsername();

                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(matchId1, player1.getUsername(), "win");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(matchId2, player2.getUsername(), "lose");
                System.out.println("[DB] player1: " + res1.getMessage());
                System.out.println("[DB] player2: " + res2.getMessage());

            } else if (score2 > score1) {
                winner = player2.getUsername();

                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(matchId1, player1.getUsername(), "lose");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(matchId2, player2.getUsername(),"win");
                System.out.println("[DB] player1: " + res1.getMessage());
                System.out.println("[DB] player2: " + res2.getMessage());

            } else {
                // Hòa
                broadcast("GAME_OVER;DRAW;" + score1);

                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(matchId1, player1.getUsername(),"draw");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(matchId2, player2.getUsername(), "draw");
                System.out.println("[DB] player1: " + res1.getMessage());
                System.out.println("[DB] player2: " + res2.getMessage());

                return;
            }

            // Gửi kết quả cho mọi client
            broadcast("GAME_OVER;" + winner + ";WIN;" + score1 + ";" + score2);

        } catch (InterruptedException e) {
            System.out.println("GameRoom bị gián đoạn.");
            isRunning = false;
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            System.err.println("GameRoom exception: " + ex.getMessage());
            ex.printStackTrace();
            // gửi lỗi tới client để họ biết
            broadcast("GAME_ERROR;INTERNAL");
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
            case "PAUSE_GAME":
                handlePauseGame(senderUsername);
                break;
            case "RESUME_GAME":
                handleResumeGame(senderUsername);
                break;
            default:
                System.out.println("Unknown game message: " + message);
        }
    }

    private void handlePauseGame(String pauserUsername) {
        int chanceLeft = 0;

        if (player1 != null && player1.getUsername().equals(pauserUsername)) {
            if (player2 == null) {
                chanceLeft = 10000;
                System.out.println("SERVER: Chế độ 1 người chơi — không trừ lượt tạm dừng.");
            } else {
                pauseChancesPlayer1--;
                chanceLeft = pauseChancesPlayer1;
            }
        } else if (player2 != null && player2.getUsername().equals(pauserUsername)) {
            pauseChancesPlayer2--;
            chanceLeft = pauseChancesPlayer2;
        } else {
            System.out.println("SERVER: Không có người chơi tồn tại!");
            return;
        }

        if (chanceLeft < 0) {
            System.out.println("SERVER: " + pauserUsername + " đã hết lượt tạm dừng!");
            broadcast("PAUSE_GAME;-1;" + chanceLeft + ";" + pauserUsername);
            return;
        }

        if (!isPaused) {
            isPaused = true;
            if (player2 == null) return;
            final int finalChanceLeft = chanceLeft;

            System.out.println("SERVER: Game đã được tạm dừng bởi " + pauserUsername);

            pauseThread = new Thread(() -> {
                int waitTime = 15;
                try {
                    for (int i = waitTime; i > 0; i--) {
                        if (!isPaused) return;
                        broadcast("PAUSE_GAME;" + i + ";" + finalChanceLeft + ";" + pauserUsername);
                        System.out.println("SERVER: Game đang tạm dừng (" + i + "s còn lại) bởi " + pauserUsername);
                        Thread.sleep(1000);
                    }

                    if (isPaused) {
                        System.out.println("SERVER: Hết 15 giây tạm dừng, tự động tiếp tục game.");
                        handleResumeGame("AUTO");
                    }
                } catch (InterruptedException e) {
                    System.out.println("SERVER: Thread pause bị ngắt — resume sớm.");
                    Thread.currentThread().interrupt();
                }
            });
            pauseThread.start();
        } else {
            System.out.println("SERVER: Game đang tạm dừng rồi — bỏ qua yêu cầu mới.");
        }
    }

    private void handleResumeGame(String resumerUsername) {
        if (isPaused) {
            isPaused = false;
            if (player2 == null) return;
            if (pauseThread != null && pauseThread.isAlive()) {
                pauseThread.interrupt();
            }

            System.out.println("SERVER: Game đã được tiếp tục bởi " + resumerUsername);
            broadcast("GAME_RESUMED;" + resumerUsername);
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
            final TrashType binType = TrashType.valueOf(binTypeName);
            final TrashType trashType = heldTrash.type;

            playerScores.compute(username, (k, v) -> {
                int currentScore = (v == null ? 0 : v);
                int newScore;

                if (trashType == binType) {
                    newScore = currentScore + 10;
                } else {
                    broadcast(String.format("WRONG_CLASSIFY;%s", username));
                    alive1P--;
                    newScore = currentScore - 5;
                }

                return Math.max(0, newScore);
            });

            int p1Score = playerScores.getOrDefault(player1.getUsername(), 0);
            int p2Score = (player2 != null) ? playerScores.getOrDefault(player2.getUsername(), 0) : 0;

            if (player2 != null) {
                broadcast(String.format("SCORE_UPDATE;%s;%d;%s;%d",
                        player1.getUsername(), p1Score,
                        player2.getUsername(), p2Score
                ));
            } else {
                broadcast(String.format("SCORE_UPDATE;%s;%d",
                        player1.getUsername(), p1Score
                ));
            }

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
        if (player1 != null) {
            try { player1.sendMessage(message); } catch (Exception e) { System.err.println("Failed sending to p1: " + e.getMessage()); }
        }
        if (player2 != null) {
            try { player2.sendMessage(message); } catch (Exception e) { System.err.println("Failed sending to p2: " + e.getMessage()); }
        }
    }

    public void broadcastUDP(String message, String senderUsername) {
        System.out.println("check_broadcastUDP: " + message);
        server.network.ClientTCPHandler receiver = null;
        if (player1 != null && player1.getUsername().equals(senderUsername)) {
            receiver = player2;
        } else {
            receiver = player1;
        }

        if (receiver == null) {
            System.out.println("SERVER WARNING: Không tìm thấy đối tượng receiver cho UDP (null). sender=" + senderUsername);
            return;
        }

        InetSocketAddress receiverAddress = receiver.getUdpAddress();
        if (receiverAddress != null) {
            server.sendUDPMessage(message, receiverAddress);
        } else {
            System.out.println("SERVER WARNING: Không tìm thấy địa chỉ UDP cho người nhận " + receiver.getUsername());
        }
    }

    public synchronized void removePlayer(String username) {
        // Dừng game nếu đang chạy
        isRunning = false;

        // Xóa điểm của người chơi rời đi
        playerScores.remove(username);

        server.removeRoom(this);
    }

    public server.network.ClientTCPHandler getPlayer1() {
        return player1;
    }

    public ClientTCPHandler getPlayer2() {
        return player2;
    }
}
