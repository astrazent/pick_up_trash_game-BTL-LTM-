package gamePlay.scenes;

import gamePlay.Main;
import gamePlay.config.GameConfig;
import gamePlay.game.*; // Import toàn bộ package game
import gamePlay.input.InputHandler;
import gamePlay.network.Client;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;

public class GameScene {
    private final Scene scene;
    private final Pane root;
    private GameLoop gameLoop;

    private Player player1;
    private Player player2;
    private final List<Trash> trashList = new ArrayList<>();
    private final List<TrashBin> trashBins = new ArrayList<>();

    // --- UI Elements ---
    private final Label scoreLabel1;
    private final Label scoreLabel2;
    private final Label timerLabel;
    private Button settingsButton; // <-- THÊM: Nút cài đặt
    private VBox pauseMenu;      // <-- THÊM: Menu tạm dừng
    private Button pauseResumeButton; // <-- THAY ĐỔI: Đổi tên để rõ ràng hơn
    private Label pauseStatusLabel;   // <-- MỚI: Thêm label để hiển thị ai đã tạm dừng
    private Label pauseChancesLabel; // Label mới để hiển thị số lượt tạm dừng còn lại

    public GameScene(int playerCount, String p1Name, String p2Name) {
        System.out.println("check_game-scene: "+p1Name+" "+p2Name);
        root = new Pane();
        GameConfig config = Main.getInstance().getGameConfig();
        scene = new Scene(root, config.window.width, config.window.height);

        // --- UI Elements ---
        scoreLabel1 = new Label(p1Name + ": 0");
        scoreLabel1.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        scoreLabel1.setTextFill(Color.WHITE);
        scoreLabel1.setTranslateX(20);
        scoreLabel1.setTranslateY(10);

        scoreLabel2 = new Label(p2Name != null ? p2Name + ": 0" : "");
        scoreLabel2.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        scoreLabel2.setTextFill(Color.WHITE);
        scoreLabel2.setTranslateX(config.window.width - 150);
        scoreLabel2.setTranslateY(10);

        timerLabel = new Label("02:00");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.layoutXProperty().bind(root.widthProperty().subtract(timerLabel.widthProperty()).divide(2));
        timerLabel.setTranslateY(10);

        root.setStyle("-fx-background-color: #333;");

        // --- Game Objects ---
        setupPlayers(playerCount, config, p1Name, p2Name);
        setupTrashBins(config);
        setupSettingsAndPauseMenu(); // <-- THÊM: Gọi phương thức thiết lập menu

        root.getChildren().addAll(scoreLabel1, timerLabel, settingsButton, pauseMenu); // Thêm các nút mới vào root


        // --- Input and GameLoop ---
        InputHandler inputHandler = new InputHandler(scene);
        gameLoop = new GameLoop(this, inputHandler, player1, player2, trashList, trashBins);
    }

    private void setupPlayers(int playerCount, GameConfig config, String p1Name, String p2Name) {
        player1 = new Player(config.window.width / 2.0 - 50, config.window.height - config.player.height - 10, p1Name);
        root.getChildren().add(player1.getView());

        double paddingRight = 50; // khoảng cách mong muốn từ lề phải

        // Player 2
        if (playerCount == 2) {
            player2 = new Player(config.window.width / 2.0 + 50, config.window.height - config.player.height - 10, p2Name);
            if (player2.getView() instanceof Rectangle) {
                ((Rectangle) player2.getView()).setFill(Color.LIGHTGREEN);
            }
            root.getChildren().add(player2.getView());

            // Score label 2
            scoreLabel2.setTranslateX(config.window.width - 150 - paddingRight); // thêm padding
            root.getChildren().add(scoreLabel2);
        }
    }

    private void setupTrashBins(GameConfig config) {
        TrashType[] types = TrashType.values();
        int binCount = types.length;

        double binWidth = (double) config.window.width / binCount;
        double binHeight = 60;
        double yPos = config.window.height - binHeight;

        for (int i = 0; i < binCount; i++) {
            double xPos = i * binWidth;
            TrashBin bin = new TrashBin(xPos, yPos, binWidth, binHeight, types[i]);
            trashBins.add(bin);
            root.getChildren().add(bin.getView());
        }
    }

    // <-- THÊM: Phương thức thiết lập nút cài đặt và menu tạm dừng
    private void setupSettingsAndPauseMenu() {
        // --- Nút cài đặt ---
        settingsButton = new Button("Tùy chọn");
        settingsButton.setFocusTraversable(false);
        settingsButton.setFont(Font.font("Arial", 16));
        settingsButton.layoutXProperty().bind(root.widthProperty().subtract(settingsButton.widthProperty()).subtract(20));
        settingsButton.setLayoutY(10);

        // --- Menu tạm dừng ---
        pauseMenu = new VBox(15);
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setStyle("-fx-background-color: rgba(40, 40, 40, 0.85); -fx-background-radius: 10; -fx-padding: 25;");

        pauseStatusLabel = new Label("");
        pauseStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        pauseStatusLabel.setTextFill(Color.WHITE);

        // --- Label mới: hiển thị số lượt dừng còn lại ---
        pauseChancesLabel = new Label("Lượt tạm dừng còn lại: 3"); // giá trị mặc định
        pauseChancesLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        pauseChancesLabel.setTextFill(Color.LIGHTGRAY);

        pauseResumeButton = new Button("Tạm dừng");
        pauseResumeButton.setFocusTraversable(false);
        pauseResumeButton.setFont(Font.font("Arial", 20));

        // Thêm label mới vào menu
        pauseMenu.getChildren().addAll(pauseStatusLabel, pauseChancesLabel, pauseResumeButton);
        pauseMenu.layoutXProperty().bind(root.widthProperty().subtract(pauseMenu.widthProperty()).divide(2));
        pauseMenu.layoutYProperty().bind(root.heightProperty().subtract(pauseMenu.heightProperty()).divide(2));
        pauseMenu.setVisible(false);

        // --- Xử lý sự kiện ---
        settingsButton.setOnAction(e -> {
            boolean currentlyVisible = pauseMenu.isVisible();

            if (currentlyVisible) {
                clearPauseMenu(); // Gọi hàm clear trước khi ẩn
            }

            pauseMenu.setVisible(!currentlyVisible);
        });

        pauseResumeButton.setOnAction(e -> {
            if (gameLoop.isPaused()) {
                Client.getInstance().requestResumeGame();
            } else {
                Client.getInstance().requestPauseGame();
            }
        });
    }
    private void clearPauseMenu() {
        pauseStatusLabel.setText("");
        pauseChancesLabel.setText("");
    }
    public void updatePauseChancesDisplay(int chancesLeft, boolean isPauser) {
        if (isPauser) {
            pauseChancesLabel.setVisible(true);
            pauseChancesLabel.setText("Lượt tạm dừng còn lại: " + chancesLeft);
        } else {
            pauseChancesLabel.setVisible(false);
        }
    }

    // <-- MỚI: Hàm được gọi bởi Client.java khi nhận được lệnh GAME_PAUSED
    public void handleGamePaused(String pauserUsername, String timeLeft, String chanceLeft) {
        String myUsername = Client.getInstance().getUsername();

        int time = Integer.parseInt(timeLeft);
        int chances = Integer.parseInt(chanceLeft);
        System.out.println("check_time_left: " + timeLeft);
        // Nếu không còn lượt dừng → chỉ hiển thị thông báo, KHÔNG pause game
        if (time == -1) {
            if (pauserUsername.equals(myUsername)) {
                pauseStatusLabel.setText("Bạn đã hết lượt tạm dừng!");
            } else {
                pauseStatusLabel.setText(pauserUsername + " đã hết lượt tạm dừng!");
            }
            updatePauseChancesDisplay(chances, false);
            return;
        }

        // Nếu game chưa bị tạm dừng, thực hiện pause
        if (!gameLoop.isPaused()) {
            gameLoop.pauseGame(); // Dừng logic game
            pauseMenu.setVisible(true);
            pauseResumeButton.setText("Tiếp tục");
            settingsButton.setDisable(true);
        }

        // Cập nhật text phù hợp với người pause
        if (pauserUsername.equals(myUsername)) {
            pauseStatusLabel.setText("Bạn đã tạm dừng trò chơi (" + timeLeft + " giây còn lại)");
            updatePauseChancesDisplay(chances, true);
        } else {
            pauseStatusLabel.setText("Game đã được tạm dừng bởi " + pauserUsername + " (" + timeLeft + " giây còn lại)");
            updatePauseChancesDisplay(chances, false);
        }
    }

    // <-- MỚI: Hàm được gọi bởi Client.java khi nhận được lệnh GAME_RESUMED
    public void handleGameResumed() {
        if (gameLoop.isPaused()) {
            gameLoop.resumeGame(); // Tiếp tục logic game
            pauseResumeButton.setText("Tạm dừng");
            pauseStatusLabel.setText("");
            pauseMenu.setVisible(false); // Ẩn menu đi
            settingsButton.setDisable(false); // Kích hoạt lại nút cài đặt
        }
    }

    public void spawnTrash(int id, double x, double y, TrashType type) {
        Trash newTrash = new Trash(id, x, y, type);
        trashList.add(newTrash);
        root.getChildren().add(newTrash.getView());
    }

    public void updateScores() {
        scoreLabel1.setText(String.format("%s: %d", player1.getUsername(), player1.getScore()));
        if (player2 != null) {
            scoreLabel2.setText(String.format("%s: %d", player2.getUsername(), player2.getScore()));
        }
    }

    public void updateOpponentPosition(String username, double x, double y) {
        System.out.printf("Check gamescene: %f %f", x, y);
        if (player2 != null && player2.getUsername().equals(username)) {
            player2.setPosition(x, y);
        }
    }

    public void updatePlayerScore(String username, int score) {
        Player player = getPlayerByName(username);
        if (player != null) {
            player.setScore(score);
            updateScores();
        }
    }

    public Player getPlayerByName(String name) {
        if (player1.getUsername().equals(name)) return player1;
        if (player2 != null && player2.getUsername().equals(name)) return player2;
        return null;
    }

    public Trash getTrashById(int id) {
        return trashList.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    public void updateTimer(int secondsLeft) {
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    public void showGameOver(String winnerName) {
        stopGameLoop();

        VBox gameOverPane = new VBox(20);
        gameOverPane.setAlignment(Pos.CENTER);
        gameOverPane.setStyle("-fx-background-color: rgba(20, 20, 20, 0.8); -fx-background-radius: 15;");

        Label gameOverLabel = new Label("GAME OVER");
        gameOverLabel.setFont(Font.font("Arial", FontWeight.BOLD, 52));
        gameOverLabel.setTextFill(Color.ORANGE);

        String resultMessage = winnerName.equalsIgnoreCase("TIE")
                ? "Kết quả: Hòa!"
                : "Người chiến thắng: " + winnerName;

        Label winnerLabel = new Label(resultMessage);
        winnerLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 28));
        winnerLabel.setTextFill(Color.WHITE);

        Button backToMenuButton = new Button("Trở về Menu");
        backToMenuButton.setFocusTraversable(false); // <-- THÊM DÒNG NÀY
        backToMenuButton.setFont(Font.font("Arial", 20));
        backToMenuButton.setOnAction(e -> Main.getInstance().showMenuScene());

        gameOverPane.getChildren().addAll(gameOverLabel, winnerLabel, backToMenuButton);

        gameOverPane.layoutXProperty().bind(root.widthProperty().subtract(gameOverPane.widthProperty()).divide(2));
        gameOverPane.layoutYProperty().bind(root.heightProperty().subtract(gameOverPane.heightProperty()).divide(2));

        root.getChildren().add(gameOverPane);
    }

    // --- Các getters và setters cơ bản ---
    public Scene getScene() {
        return scene;
    }

    public void startGameLoop() {
        gameLoop.start();
    }

    public void stopGameLoop() {
        gameLoop.stop();
    }
}