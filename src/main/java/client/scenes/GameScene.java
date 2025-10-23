package client.scenes;

import client.Main;
import client.config.GameConfig;
import client.game.*;
import client.input.InputHandler;
import client.network.Client;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private Label scoreLabel2; // <-- THAY ĐỔI: Có thể không được khởi tạo
    private final Label timerLabel;
    private Button settingsButton;
    private VBox pauseMenu;
    private Button pauseResumeButton;
    private Label pauseStatusLabel;
    private Label pauseChancesLabel;

    // --- THÊM: Các yếu tố UI cho chế độ 1 người chơi ---
    private HBox heartsBox; // Container cho các trái tim
    private final List<ImageView> heartImageViews = new ArrayList<>();
    private int playerLives = 3; // Số mạng ban đầu

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

        timerLabel = new Label("02:00");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.layoutXProperty().bind(root.widthProperty().subtract(timerLabel.widthProperty()).divide(2));
        timerLabel.setTranslateY(10);

        root.setStyle("-fx-background-color: #333;");

        // --- Game Objects ---
        setupPlayers(playerCount, config, p1Name, p2Name);
        setupTrashBins(config);
        setupSettingsAndPauseMenu();

        root.getChildren().addAll(scoreLabel1, timerLabel, settingsButton, pauseMenu);


        // --- Input and GameLoop ---
        InputHandler inputHandler = new InputHandler(scene);
        gameLoop = new GameLoop(this, inputHandler, player1, player2, trashList, trashBins);
    }

    private void setupPlayers(int playerCount, GameConfig config, String p1Name, String p2Name) {
        player1 = new Player(config.window.width / 2.0 - 50, config.window.height - config.player.height - 10, p1Name);
        root.getChildren().add(player1.getView());

        double paddingRight = 50; // khoảng cách mong muốn từ lề phải

        if (playerCount == 2) {
            // Player 2
            player2 = new Player(config.window.width / 2.0 + 50, config.window.height - config.player.height - 10, p2Name);
            if (player2.getView() instanceof Rectangle) {
                ((Rectangle) player2.getView()).setFill(Color.LIGHTGREEN);
            }
            root.getChildren().add(player2.getView());

            // Score label 2
            scoreLabel2 = new Label(p2Name + ": 0");
            scoreLabel2.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            scoreLabel2.setTextFill(Color.WHITE);
            scoreLabel2.setTranslateX(config.window.width - 150 - paddingRight); // thêm padding
            root.getChildren().add(scoreLabel2);
        } else {
            // --- MỚI: Thiết lập hiển thị mạng cho chế độ 1 người chơi ---
            setupLivesDisplay(config);
        }
    }

    // --- MỚI: Phương thức thiết lập hiển thị mạng (trái tim) ---
    private void setupLivesDisplay(GameConfig config) {
        heartsBox = new HBox(5); // 5 là khoảng cách giữa các trái tim
        heartsBox.setAlignment(Pos.CENTER);
        double heartsBoxX = config.window.width - 215; // Vị trí tương tự scoreLabel2
        double heartsBoxY = 10;
        heartsBox.setLayoutX(heartsBoxX);
        heartsBox.setLayoutY(heartsBoxY);

        try {
            // Thay "resources/heart.png" bằng đường dẫn chính xác đến file ảnh của bạn
            Image heartImage = new Image(new FileInputStream("src/main/resources/images/heart.png"));
            for (int i = 0; i < 3; i++) { // Luôn tạo 3 trái tim
                ImageView heartView = new ImageView(heartImage);
                heartView.setFitHeight(30);
                heartView.setFitWidth(30);
                heartImageViews.add(heartView);
                heartsBox.getChildren().add(heartView);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Không tìm thấy file ảnh trái tim! 'resources/heart.png'");
            // Thay thế bằng text nếu không có ảnh
            Label livesLabel = new Label("Mạng: " + playerLives);
            livesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            livesLabel.setTextFill(Color.RED);
            heartsBox.getChildren().add(livesLabel);
        }

        root.getChildren().add(heartsBox);
    }

    // --- MỚI: Phương thức để xử lý khi người chơi mất một mạng ---
    public void playerLosesLife(String playerName) {
        if (!playerName.equals(player1.getUsername())) {
            System.out.println("Lỗi: tên người chơi không hợp lệ (" + playerName + ")");
            return;
        }

        if (playerLives > 0) {
            playerLives--;
            updateLivesDisplay();

            if (playerLives <= 0) {
                showGameOver(player1.getUsername());
            }
        }
    }

    // --- MỚI: Cập nhật giao diện hiển thị số mạng còn lại ---
    private void updateLivesDisplay() {
        // Ẩn trái tim dựa trên số mạng còn lại
        for (int i = 0; i < heartImageViews.size(); i++) {
            if (i < playerLives) {
                heartImageViews.get(i).setVisible(true);
            } else {
                heartImageViews.get(i).setVisible(false);
            }
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

    private void setupSettingsAndPauseMenu() {
        settingsButton = new Button("Tùy chọn");
        settingsButton.setFocusTraversable(false);
        settingsButton.setFont(Font.font("Arial", 16));
        settingsButton.layoutXProperty().bind(root.widthProperty().subtract(settingsButton.widthProperty()).subtract(20));
        settingsButton.setLayoutY(10);

        pauseMenu = new VBox(15);
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setStyle("-fx-background-color: rgba(40, 40, 40, 0.85); -fx-background-radius: 10; -fx-padding: 25;");

        pauseStatusLabel = new Label("");
        pauseStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        pauseStatusLabel.setTextFill(Color.WHITE);

        // Luôn khởi tạo pauseChancesLabel, nhưng tùy player2 để bật/tắt
        pauseChancesLabel = new Label("Lượt tạm dừng còn lại: 3");
        pauseChancesLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        pauseChancesLabel.setTextFill(Color.LIGHTGRAY);

        if (player2 == null) {
            pauseChancesLabel.setDisable(true);
            pauseChancesLabel.setVisible(false); // ẩn hẳn khỏi giao diện
        }

        pauseResumeButton = new Button("Tạm dừng");
        pauseResumeButton.setFocusTraversable(false);
        pauseResumeButton.setFont(Font.font("Arial", 20));

        pauseMenu.getChildren().addAll(pauseStatusLabel, pauseChancesLabel, pauseResumeButton);

        pauseMenu.layoutXProperty().bind(root.widthProperty().subtract(pauseMenu.widthProperty()).divide(2));
        pauseMenu.layoutYProperty().bind(root.heightProperty().subtract(pauseMenu.heightProperty()).divide(2));
        pauseMenu.setVisible(false);

        settingsButton.setOnAction(e -> {
            boolean currentlyVisible = pauseMenu.isVisible();
            if (currentlyVisible) {
                clearPauseMenu();
            }
            pauseMenu.setVisible(!currentlyVisible);
        });

        // Xử lý logic nút tạm dừng
        if (player2 == null) {
            pauseResumeButton.setOnAction(e -> {
                if (gameLoop.isPaused()) {
                    gameLoop.resumeGame();
                    Client.getInstance().requestResumeGame();
                    pauseResumeButton.setText("Tạm dừng");
                    pauseStatusLabel.setText("");
                    pauseMenu.setVisible(false);
                    settingsButton.setDisable(false);
                } else {
                    gameLoop.pauseGame();
                    Client.getInstance().requestPauseGame();
                    pauseResumeButton.setText("Tiếp tục");
                    pauseStatusLabel.setText("Trò chơi đã tạm dừng");
                    settingsButton.setDisable(true);
                }
            });
        } else {
            pauseResumeButton.setOnAction(e -> {
                if (gameLoop.isPaused()) {
                    Client.getInstance().requestResumeGame();
                } else {
                    Client.getInstance().requestPauseGame();
                }
            });
        }
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

    public void handleGamePaused(String pauserUsername, String timeLeft, String chanceLeft) {
        String myUsername = Client.getInstance().getUsername();

        int time = Integer.parseInt(timeLeft);
        int chances = Integer.parseInt(chanceLeft);
        System.out.println("check_time_left: " + timeLeft);

        if (time == -1) {
            if (pauserUsername.equals(myUsername)) {
                pauseStatusLabel.setText("Bạn đã hết lượt tạm dừng!");
            } else {
                pauseStatusLabel.setText(pauserUsername + " đã hết lượt tạm dừng!");
            }
            updatePauseChancesDisplay(chances, false);
            return;
        }

        if (!gameLoop.isPaused()) {
            gameLoop.pauseGame();
            pauseMenu.setVisible(true);
            pauseResumeButton.setText("Tiếp tục");
            settingsButton.setDisable(true);
        }

        if (pauserUsername.equals(myUsername)) {
            pauseStatusLabel.setText("Bạn đã tạm dừng trò chơi (" + timeLeft + " giây còn lại)");
            updatePauseChancesDisplay(chances, true);
        } else {
            pauseStatusLabel.setText("Game đã được tạm dừng bởi " + pauserUsername + " (" + timeLeft + " giây còn lại)");
            updatePauseChancesDisplay(chances, false);
        }
    }

    public void handleGameResumed() {
        if (gameLoop.isPaused()) {
            gameLoop.resumeGame();
            pauseResumeButton.setText("Tạm dừng");
            pauseStatusLabel.setText("");
            pauseMenu.setVisible(false);
            settingsButton.setDisable(false);
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

        String resultMessage;

        // 1. Chế độ 1 người
        if (player2 == null) {
            if (playerLives <= 0) {
                resultMessage = "Bạn đã hết mạng!";
            } else {
                resultMessage = "Trò chơi kết thúc!";
            }
        }
        // 2. Chế độ 2 người
        else {
            if (winnerName.equalsIgnoreCase("TIE")) {
                resultMessage = "Kết quả: Hòa!";
            } else if (winnerName.equalsIgnoreCase(player1.getUsername())) {
                resultMessage = "Chúc mừng! Bạn đã thắng 🎉";
            } else {
                resultMessage = "Bạn đã thua 😢";
            }
        }

        Label winnerLabel = new Label(resultMessage);
        winnerLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 28));
        winnerLabel.setTextFill(Color.WHITE);

        Button backToMenuButton = new Button("Trở về Menu");
        backToMenuButton.setFocusTraversable(false);
        backToMenuButton.setFont(Font.font("Arial", 20));
        backToMenuButton.setOnAction(e -> Main.getInstance().showMenuScene());

        gameOverPane.getChildren().addAll(gameOverLabel, winnerLabel, backToMenuButton);

        gameOverPane.layoutXProperty().bind(root.widthProperty().subtract(gameOverPane.widthProperty()).divide(2));
        gameOverPane.layoutYProperty().bind(root.heightProperty().subtract(gameOverPane.heightProperty()).divide(2));

        root.getChildren().add(gameOverPane);
    }

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