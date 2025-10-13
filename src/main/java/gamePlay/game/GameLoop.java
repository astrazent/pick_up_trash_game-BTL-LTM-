package gamePlay.game;

import gamePlay.Main;
import gamePlay.config.GameConfig;
import gamePlay.input.InputHandler;
import gamePlay.network.Client;
import gamePlay.scenes.GameScene;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import java.util.List;

public class GameLoop extends AnimationTimer {

    private final GameScene gameScene;
    private final InputHandler inputHandler;
    private final Player player1;
    private final Player player2;
    private final List<Trash> trashList;
    private final List<TrashBin> trashBins;

    private final double screenWidth;
    private final double screenHeight; // <-- THÊM BIẾN screenHeight
    private long lastTrashSpawnTime = 0;
    private final long trashSpawnInterval;

    public GameLoop(GameScene gameScene, InputHandler inputHandler, Player player1, Player player2, List<Trash> trashList, List<TrashBin> trashBins) {
        this.gameScene = gameScene;
        this.inputHandler = inputHandler;
        this.player1 = player1;
        this.player2 = player2;
        this.trashList = trashList;
        this.trashBins = trashBins;

        GameConfig config = Main.getInstance().getGameConfig();
        this.screenWidth = config.window.width;
        this.screenHeight = config.window.height; // <-- LẤY CHIỀU CAO MÀN HÌNH
        this.trashSpawnInterval = (long) config.trash.spawn_rate_ms * 1_000_000;
    }

    @Override
    public void handle(long now) {
        // 1. Xử lý Input và di chuyển Player 1
        // Di chuyển ngang
        if (inputHandler.isKeyPressed(KeyCode.A) || inputHandler.isKeyPressed(KeyCode.LEFT)) {
            player1.moveLeft();
        }
        if (inputHandler.isKeyPressed(KeyCode.D) || inputHandler.isKeyPressed(KeyCode.RIGHT)) {
            player1.moveRight(screenWidth);
        }
        // THÊM: Di chuyển dọc
        if (inputHandler.isKeyPressed(KeyCode.W) || inputHandler.isKeyPressed(KeyCode.UP)) {
            player1.moveUp();
        }
        if (inputHandler.isKeyPressed(KeyCode.S) || inputHandler.isKeyPressed(KeyCode.DOWN)) {
            player1.moveDown(screenHeight);
        }

        // Gửi thông tin vị trí lên server
        if (player2 != null) {
            // UDP không tin cậy, nên gửi cả x và y
            String message = String.format("UPDATE_POS;%s;%f;%f", player1.getUsername(), player1.getX(), player1.getY());
            Client.getInstance().sendUDPMessage(message);
        }

        // 2. Cập nhật các đối tượng trong game
        player1.update();
        if (player2 != null) {
            player2.update();
        }

        for (Trash trash : trashList) {
            if (!player1.isHoldingTrash(trash) && (player2 == null || !player2.isHoldingTrash(trash))) {
                trash.update();
            }
        }

        // 3. Xử lý va chạm VÀ LOGIC THẢ RÁC
        checkCollisionsAndActions(); // Đổi tên hàm cho rõ nghĩa hơn

        // 4. Reset rác nếu rơi ra ngoài màn hình
        for (Trash trash : trashList) {
            if (trash.getY() > screenHeight) {
                trash.resetPosition(screenWidth);
            }
        }

        // 5. Tạo rác mới theo thời gian
        if (now - lastTrashSpawnTime > trashSpawnInterval && trashList.size() < 2) {
            gameScene.spawnTrash();
            lastTrashSpawnTime = now;
        }

        // 6. Cập nhật UI (điểm số)
        gameScene.updateScores();
    }

    private void checkCollisionsAndActions() {
        handlePlayerLogic(player1);
        if(player2 != null) {
            // Logic cho player2 sẽ cần được xử lý thông qua tin nhắn từ server
            // để tránh xung đột. Tạm thời chỉ xử lý va chạm cho player1.
        }
    }

    // --- HÀM LOGIC CHÍNH ĐƯỢC SỬA ĐỔI ---
    private void handlePlayerLogic(Player player) {
        // A. NẾU NGƯỜI CHƠI ĐANG KHÔNG CẦM RÁC -> KIỂM TRA NHẶT RÁC
        if (!player.isHoldingTrash()) {
            for (Trash trash : trashList) {
                // Rác chạm vào người chơi là nhặt
                if (player.checkCollision(trash)) {
                    player.pickUpTrash(trash);
                    break; // Chỉ nhặt 1 rác mỗi lần
                }
            }
        }
        // B. NẾU NGƯỜI CHƠI ĐANG CẦM RÁC VÀ BẤM ENTER -> KIỂM TRA THẢ VÀO THÙNG
        else if (inputHandler.isKeyPressed(KeyCode.ENTER)) {
            Trash heldTrash = player.getHeldTrash();
            boolean dropped = false; // Biến kiểm tra xem đã thả rác chưa

            for (TrashBin bin : trashBins) {
                // Kiểm tra xem người chơi có đang ở trên thùng rác không
                if (player.checkCollision(bin)) {
                    // Kiểm tra xem có thả đúng loại rác không
                    if (heldTrash.getType() == bin.getBinType()) {
                        player.incrementScore(); // Cộng điểm
                    } else {
                        player.decrementScore(); // Trừ điểm
                    }
                    heldTrash.resetPosition(screenWidth); // Reset vị trí rác
                    player.dropTrash(); // Người chơi thả rác
                    dropped = true;
                    break; // Thoát khỏi vòng lặp vì đã xử lý xong
                }
            }

            // Nếu người chơi bấm Enter nhưng không đứng trên thùng rác nào
            // bạn có thể thêm logic phạt ở đây nếu muốn. Hiện tại, không làm gì cả.
        }
    }
}