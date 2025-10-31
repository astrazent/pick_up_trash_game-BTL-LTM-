package server.network;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import client.game.TrashType;
import server.utils.DatabaseConnector;
import server.utils.DatabaseResponse;

class TrashState {
    public int id;
    public double x, y;
    public TrashType type;
    public int imageIndex; // Index của ảnh trong danh sách ảnh của type đó
    public String heldBy = null;

    public TrashState(int id, double x, double y, TrashType type, int imageIndex) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.imageIndex = imageIndex;
    }
}

public class GameRoom implements Runnable {
    private final server.network.ClientTCPHandler player1;
    public final server.network.ClientTCPHandler player2;
    private final GameServer server;
    private final String roomCode; // Mã phòng (6 ký tự)
    private Thread pauseThread;
    private volatile boolean isRunning = false; // MỚI: Sử dụng volatile để đảm bảo an toàn luồng
    private volatile boolean isPaused = false;  // MỚI: Biến trạng thái để kiểm soát việc tạm dừng game
    private volatile boolean gameEndedBySurrender = false; // MỚI: Đánh dấu game kết thúc do đầu hàng

    private final int gameDurationSeconds = 120;
    private final long trashSpawnIntervalMs = 5000;
    private int pauseChancesPlayer1 = 3; // Số lượt dừng còn lại player 1
    private int pauseChancesPlayer2 = 3; // Số lượt dừng còn lại player 2
    private int alive1P = 3; //tổng số cơ hội phân loại sai tối đa

    private static final AtomicInteger trashIdCounter = new AtomicInteger(0);

    private final Map<Integer, TrashState> trashStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();

    public GameRoom(server.network.ClientTCPHandler p1, server.network.ClientTCPHandler p2, GameServer server) {
        this.player1 = p1;
        this.player2 = p2;
        System.out.println("h1: " + System.identityHashCode(p1) + " h2: " + System.identityHashCode(p2));
        this.server = server;
        playerScores.put(p1.getUsername(), 0);
        playerScores.put(p2.getUsername(), 0);
        this.roomCode = "2P" + generateRoomCode();
    }

    public GameRoom(server.network.ClientTCPHandler p1, GameServer server) {
        this.player1 = p1;
        this.player2 = null; // Quan trọng: Đánh dấu đây là phòng 1 người chơi
        this.server = server;
        playerScores.put(p1.getUsername(), 0);
        this.roomCode = "1P" + generateRoomCode();
    }

    // Hàm tạo mã phòng ngẫu nhiên 6 ký tự (chữ + số in hoa)
    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }

    // Getter cho mã phòng (không có setter)
    public String getRoomCode() {
        return roomCode;
    }

    @Override
    public void run() {
        try {
            isRunning = true;
            if (player2 != null) {
                System.out.println("SERVER: GameRoom bắt đầu giữa " + player1.getUsername() + " và " + player2.getUsername());
                broadcast("START_GAME;" + player1.getUsername() + ";" + player2.getUsername());
            } else {
                System.out.println("SERVER: GameRoom (1P) bắt đầu cho " + player1.getUsername());
                broadcast("START_GAME;" + player1.getUsername());
            }

            int secondsLeft = gameDurationSeconds;
            long lastTrashSpawnTime = System.currentTimeMillis();

            while (isRunning && secondsLeft > 0) {
                // MỚI: Chỉ xử lý logic game nếu không bị tạm dừng
                if (!isPaused) {
                    Thread.sleep(1000);
                    secondsLeft--;

                    broadcast("TIMER_UPDATE;" + secondsLeft);

                    if (System.currentTimeMillis() - lastTrashSpawnTime > trashSpawnIntervalMs) {
                        spawnNewTrash();
                        lastTrashSpawnTime = System.currentTimeMillis();
                    }
                } else {
                    // Khi game tạm dừng, luồng sẽ nghỉ một chút để không chiếm dụng CPU
                    Thread.sleep(100);
                }
            }
            
            // Kiểm tra nếu game kết thúc do đầu hàng, không cần so sánh điểm
            if (gameEndedBySurrender) {
                System.out.println("SERVER: Game đã kết thúc do đầu hàng, bỏ qua so sánh điểm.");
                return;
            }
            
            // Lấy điểm hiện tại của hai người chơi
            int score1 = playerScores.getOrDefault(player1.getUsername(), 0);
            int score2 = 0;

            if (player2 != null) {
                score2 = playerScores.getOrDefault(player2.getUsername(), 0);
            } else {
                // Nếu không có player2, 1P thắng mặc định
                System.out.println("SERVER: [Player: " + player1.getUsername() + "] complete 1P match");

                // Cập nhật điểm player1 (thắng)
                DatabaseResponse<Void> res = DatabaseConnector.updateScoreAfterMatch(player1.getUsername(), player1.getUsername(), "win");
                System.out.println(res.getMessage());

                broadcast("GAME_OVER;" + player1.getUsername());
                return;
            }

            String winner;
            if (score1 > score2) {
                winner = player1.getUsername();

                // Cập nhật điểm (Player 1 thắng, Player 2 thua)
                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(player1.getUsername(), player2.getUsername(), "win");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(player2.getUsername(), player1.getUsername(), "lose");
                System.out.println(res1.getMessage());
                System.out.println(res2.getMessage());

            } else if (score2 > score1) {
                winner = player2.getUsername();

                // Cập nhật điểm (Player 1 thua, Player 2 thắng)
                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(player1.getUsername(), player2.getUsername(), "lose");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(player2.getUsername(), player1.getUsername(), "win");
                System.out.println(res1.getMessage());
                System.out.println(res2.getMessage());

            } else {
                // Hòa
                broadcast("GAME_OVER;DRAW;" + score1);

                // Cập nhật điểm cho cả hai người chơi (cả hai đều hòa)
                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(player1.getUsername(), player2.getUsername(), "draw");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(player2.getUsername(), player1.getUsername(), "draw");
                System.out.println(res1.getMessage());
                System.out.println(res2.getMessage());

                return;
            }

            // Gửi kết quả cho mọi client
            broadcast("GAME_OVER;" + winner + ";WIN;" + score1 + ";" + score2);

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
        
        // Random chọn index của ảnh
        int imageIndex = (int) (Math.random() * getImageCountForType(type));

        trashStates.put(id, new TrashState(id, x, y, type, imageIndex));

        String message = String.format("TRASH_SPAWN;%d;%f;%f;%s;%d", id, x, y, type.name(), imageIndex);
        broadcast(message);
    }
    
    // Helper method để lấy số lượng ảnh cho mỗi type
    private int getImageCountForType(TrashType type) {
        switch (type) {
            case METAL:
                return 6; // metals có 6 ảnh
            case ORGANIC:
                return 8; // organics có 8 ảnh
            case PAPER:
                return 7; // papers có 7 ảnh
            case PLASTIC:
                return 7; // plastics có 7 ảnh
            default:
                return 1;
        }
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
                //dữ liệu nhận: PAUSE_GAME;player1
                handlePauseGame(senderUsername);
                break;
            case "RESUME_GAME":
                handleResumeGame(senderUsername);
                break;
            case "SURRENDER":
                handleSurrender(senderUsername);
                break;
        }
    }

    // MỚI: Hàm xử lý yêu cầu tạm dừng game
    private void handlePauseGame(String pauserUsername) {
        int chanceLeft = 0; // <-- khai báo trước

        // Xác định người tạm dừng
        if (player1 != null && player1.getUsername().equals(pauserUsername)) {
            if (player2 == null) {
                chanceLeft = 10000;
                System.out.println("SERVER: Chế độ 1 người chơi — không trừ lượt tạm dừng.");
            } else {
                pauseChancesPlayer1--;
                chanceLeft = pauseChancesPlayer1; // gán giá trị
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
            final int finalChanceLeft = chanceLeft; // <- copy sang biến final để dùng trong lambda

            System.out.println("SERVER: Game đã được tạm dừng bởi " + pauserUsername);

            pauseThread = new Thread(() -> {
                int waitTime = 15;
                try {
                    for (int i = waitTime; i > 0; i--) {
                        if (!isPaused) return; // <-- nếu resume sớm thì thoát khỏi vòng lặp
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
                    Thread.currentThread().interrupt(); // best practice
                }
            });
            pauseThread.start();
        } else {
            System.out.println("SERVER: Game đang tạm dừng rồi — bỏ qua yêu cầu mới.");
        }
    }


    // MỚI (Khuyến nghị): Hàm xử lý yêu cầu tiếp tục game
    private void handleResumeGame(String resumerUsername) {
        if (isPaused) {
            isPaused = false;
            if (player2 == null) return;
            // Ngắt thread đếm ngược nếu còn chạy
            if (pauseThread != null && pauseThread.isAlive()) {
                pauseThread.interrupt();
            }

            System.out.println("SERVER: Game đã được tiếp tục bởi " + resumerUsername);
            broadcast("GAME_RESUMED;" + resumerUsername);
        }
    }

    // MỚI: Hàm xử lý khi người chơi đầu hàng (thoát)
    private void handleSurrender(String surrenderUsername) {
        System.out.println("SERVER: " + surrenderUsername + " đã đầu hàng!");
        
        // Đánh dấu game kết thúc do đầu hàng
        gameEndedBySurrender = true;
        
        // Dừng game ngay lập tức
        isRunning = false;
        
        // Xác định người chiến thắng (người còn lại)
        String winner;
        int score1 = playerScores.getOrDefault(player1.getUsername(), 0);
        int score2 = 0;
        
        if (player2 != null) {
            score2 = playerScores.getOrDefault(player2.getUsername(), 0);
            
            if (surrenderUsername.equals(player1.getUsername())) {
                winner = player2.getUsername();
                
                // Cập nhật điểm vào database (Player 1 thua, Player 2 thắng)
                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(player1.getUsername(), player2.getUsername(), "lose");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(player2.getUsername(), player1.getUsername(), "win");
                System.out.println(res1.getMessage());
                System.out.println(res2.getMessage());
            } else {
                winner = player1.getUsername();
                
                // Cập nhật điểm vào database (Player 1 thắng, Player 2 thua)
                DatabaseResponse<Void> res1 = DatabaseConnector.updateScoreAfterMatch(player1.getUsername(), player2.getUsername(), "win");
                DatabaseResponse<Void> res2 = DatabaseConnector.updateScoreAfterMatch(player2.getUsername(), player1.getUsername(), "lose");
                System.out.println(res1.getMessage());
                System.out.println(res2.getMessage());
            }
            
            // Gửi thông báo kết thúc game
            broadcast("GAME_OVER;" + winner + ";WIN;" + score1 + ";" + score2);
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
            final TrashType trashType = heldTrash.type; // <- thêm dòng này

            // Cập nhật điểm
            playerScores.compute(username, (k, v) -> {
                int currentScore = (v == null ? 0 : v);
                int newScore;

                if (trashType == binType) {
                    newScore = currentScore + 10;
                } else {
                    broadcast(String.format("WRONG_CLASSIFY;%s", username));
                    
                    // CHỈ giảm alive1P trong mode 1 player
                    if (player2 == null) {
                        alive1P--;
                    }
                    
                    newScore = currentScore - 5;
                }

                // Không cho điểm âm
                return Math.max(0, newScore);
            });

            // Lấy điểm hiện tại
            int p1Score = playerScores.getOrDefault(player1.getUsername(), 0);
            int p2Score = (player2 != null) ? playerScores.getOrDefault(player2.getUsername(), 0) : 0;

            // Gửi cập nhật điểm
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

            // Gửi thông báo rác đã được thả và xóa khỏi màn hình
            broadcast("TRASH_DROPPED;" + username + ";" + trashId);
            
            // Xóa rác khỏi danh sách
            trashStates.remove(trashId);
            
            // Gửi message để client xóa rác
            broadcast("TRASH_REMOVED;" + trashId);
        }
    }

    public void broadcast(String message) {
        if (player1 != null) {
            player1.sendMessage(message);
        }
        // Chỉ gửi cho player2 nếu player2 tồn tại
        if (player2 != null) {
            player2.sendMessage(message);
        }
    }

    // MỚI: Phương thức broadcast tin nhắn chat
    public void broadcastChatMessage(String senderUsername, String message) {
        System.out.println("[CHAT] " + senderUsername + ": " + message);
        
        // Format: CHAT_MESSAGE;senderUsername;message
        String chatMessage = "CHAT_MESSAGE;" + senderUsername + ";" + message;
        
        // Gửi tin nhắn chat đến tất cả người chơi trong phòng
        if (player1 != null) {
            player1.sendMessage(chatMessage);
        }
        if (player2 != null) {
            player2.sendMessage(chatMessage);
        }
    }

    public void broadcastUDP(String message, String senderUsername) {
        // 1. Xác định người nhận là ai
        server.network.ClientTCPHandler receiver;
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
