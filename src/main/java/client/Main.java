package client;

import client.config.GameConfig;
import server.config.NetworkConfig;
import client.network.Client;
import client.scenes.*;
import client.utils.ResourceLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Main instance;
    private static Stage primaryStage;
    private GameConfig gameConfig;
    private LoginScene loginScene;
    private Scene currentScene;

    private HistoryScene historyScene;
    private LeaderboardScene leaderboardScene;

    // --- BIẾN QUAN TRỌNG CẦN THÊM ---
    private GameScene activeGameScene; // Sẽ lưu đối tượng GameScene đang chạy
    private LeaderboardScene activeLeaderboardScene;
    private HistoryScene activeHistoryScene; // <-- THÊM BIẾN NÀY
    private MenuScene activeMenuScene;
    private WaitingAcceptanceScene activeWaitingAcceptanceScene;
    public Main() { instance = this; }
    public static Main getInstance() { return instance; }

    // --- PHƯƠNG THỨC MỚI CẦN THÊM ---
    public GameScene getActiveGameScene() {
        return activeGameScene;
    }
    public LeaderboardScene getActiveLeaderboardScene() {
        return activeLeaderboardScene;
    }
    public MenuScene getActiveMenuScene() {return activeMenuScene; }
    public WaitingAcceptanceScene getActiveWaitingAcceptanceScene() {return activeWaitingAcceptanceScene; }

    public HistoryScene getActiveHistoryScene() {
        return activeHistoryScene;
    }
    @Override
    public void start(Stage stage) {
        // 1. Lưu lại cửa sổ chính để có thể sử dụng ở các nơi khác
        primaryStage = stage;

        // 2. Tải các file cấu hình
        gameConfig = ResourceLoader.loadGameConfig();
        NetworkConfig networkConfig = ResourceLoader.loadNetworkConfig();

        // 3. Kiểm tra nếu tải config thất bại thì dừng chương trình
        if (gameConfig == null || networkConfig == null) {
            System.err.println("Không thể tải file cấu hình. Thoát ứng dụng.");
            // Có thể hiện một Alert ở đây để thông báo cho người dùng
            return;
        }

        historyScene = new HistoryScene();
        leaderboardScene = new LeaderboardScene();

        // 4. Khởi tạo và kết nối Client mạng
        // LƯU Ý: Dòng connect() này sẽ làm chương trình bị "treo"
        // nếu Server chưa được chạy. Đây là hành vi đúng.
        Client.initialize(networkConfig);
        try {
            Client.getInstance().connect();
        } catch (Exception e) {
            System.err.println("Không thể kết nối tới server: " + e.getMessage());
            // Có thể hiện một Alert ở đây
            return; // Dừng nếu không kết nối được
        }

        // 5. Cấu hình cửa sổ game
        primaryStage.setTitle(gameConfig.window.title);

        // 6. Hiển thị màn hình đầu tiên (Login)
        showLoginScene();

        // 7. Hiển thị cửa sổ ra màn hình
        primaryStage.show();
    }

    // --- CẬP NHẬT CÁC HÀM "showScene" ---

    public void showLoginScene() {
        this.activeGameScene = null; // Không có game nào đang chạy
        loginScene = new LoginScene();
        this.currentScene = loginScene.getScene();
        primaryStage.setScene(this.currentScene);
    }

    public void showMenuScene() {
        this.activeGameScene = null; // Không có game nào đang chạy
        if (this.activeMenuScene == null) {
            this.activeMenuScene = new MenuScene();
        }
        this.currentScene = this.activeMenuScene.getScene();
        primaryStage.setScene(this.currentScene);
    }

    public void showWaitingScene() {
        this.activeGameScene = null; // Không có game nào đang chạy
        WaitingScene waitingScene = new WaitingScene();
        this.currentScene = waitingScene.getScene();
        primaryStage.setScene(this.currentScene);
    }

    public void showWaitingAcceptanceScene(String opponentName) {
        this.activeGameScene = null; // Không có game nào đang chạy
        WaitingAcceptanceScene waitingAcceptanceScene = new WaitingAcceptanceScene(opponentName);
        this.currentScene = waitingAcceptanceScene.getScene();
        primaryStage.setScene(this.currentScene);
    }

    // Sửa hàm showGameScene (2 người chơi)
    public void showGameScene(int playerCount, String p1Name, String p2Name) {
        GameScene gameScene = new GameScene(playerCount, p1Name, p2Name);
        this.activeGameScene = gameScene; // <-- LƯU LẠI THAM CHIẾU
        this.currentScene = gameScene.getScene();
        primaryStage.setScene(this.currentScene);
        gameScene.startGameLoop();
    }

    // Thêm phương thức này vào lớp Main.java
    public void showLeaderboardScene() {
        primaryStage.setScene(leaderboardScene.getScene());
        this.activeLeaderboardScene = leaderboardScene;
        leaderboardScene.refreshData();
        primaryStage.setTitle("Leaderboard");
    }

    public void showHistoryScene() {
        // Không cần kiểm tra if (this.historyScene == null) nữa vì nó đã được khởi tạo trong constructor
        primaryStage.setScene(historyScene.getScene());
        this.activeHistoryScene = historyScene;
        historyScene.refreshData(); // GỌI refreshData() Ở ĐÂY KHI CHUYỂN SCENE
        primaryStage.setTitle("Match History");
    }

    // Sửa hàm showGameScene (1 người chơi)
    public void showGameScene(int playerCount) {
        if (playerCount == 1) {
            String myUsername = Client.getInstance().getUsername();
            GameScene gameScene = new GameScene(playerCount, myUsername, null);
            // Gửi trạng thái sẵn sàng và SỐ LƯỢNG NGƯỜI CHƠI lên server
            String username = Client.getInstance().getUsername();
            if (username != null) {
                // ĐỊNH DẠNG MỚI: READY;username;playerCount
                Client.getInstance().sendMessage("READY;" + username + ";1");
            } else {
                // Xử lý lỗi nếu không có username (về lại màn login)
                Main.getInstance().showLoginScene();
            }
            this.activeGameScene = gameScene; // <-- LƯU LẠI THAM CHIẾU
            this.currentScene = gameScene.getScene();
            primaryStage.setScene(this.currentScene);
            gameScene.startGameLoop();
        }
    }

    public Scene getCurrentScene() { return this.currentScene; }
    public GameConfig getGameConfig() { return gameConfig; }
    public LoginScene getLoginScene() { return loginScene; }

    @Override
    public void stop() { Client.getInstance().close(); }
    public static void main(String[] args) { launch(args); }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}