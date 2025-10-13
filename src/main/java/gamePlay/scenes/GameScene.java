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
    private final Label timerLabel; // <-- THÊM: Label cho đồng hồ đếm ngược

    public GameScene(int playerCount, String p1Name, String p2Name) {
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

        // <-- THÊM: Khởi tạo Timer Label
        timerLabel = new Label("02:00");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        timerLabel.setTextFill(Color.WHITE);
        // Căn giữa label theo chiều ngang
        timerLabel.layoutXProperty().bind(root.widthProperty().subtract(timerLabel.widthProperty()).divide(2));
        timerLabel.setTranslateY(10);


        root.setStyle("-fx-background-color: #333;");
        root.getChildren().addAll(scoreLabel1, timerLabel); // Thêm timerLabel vào root

        // --- Game Objects ---
        setupPlayers(playerCount, config, p1Name, p2Name);
        setupTrashBins(config);

        // <-- XÓA: Client không tự tạo rác ban đầu nữa. Server sẽ gửi lệnh.
        // for (int i = 0; i < 5; i++) {
        //     spawnTrash();
        // }

        // --- Input and GameLoop ---
        InputHandler inputHandler = new InputHandler(scene);
        gameLoop = new GameLoop(this, inputHandler, player1, player2, trashList, trashBins);
    }

    private void setupPlayers(int playerCount, GameConfig config, String p1Name, String p2Name) {
        player1 = new Player(config.window.width / 2.0 - 50, config.window.height - config.player.height - 10, p1Name);
        root.getChildren().add(player1.getView());

        if (playerCount == 2) {
            player2 = new Player(config.window.width / 2.0 + 50, config.window.height - config.player.height - 10, p2Name);
            if (player2.getView() instanceof Rectangle) {
                ((Rectangle) player2.getView()).setFill(Color.LIGHTGREEN);
            }
            root.getChildren().add(player2.getView());
            root.getChildren().add(scoreLabel2);
        }
    }

    private void setupTrashBins(GameConfig config) {
        // Giả sử có 3 loại rác, tính toán chiều rộng mỗi thùng
        double binWidth = config.window.width / 3.0;
        double binHeight = 60;
        double yPos = config.window.height - binHeight;

        TrashType[] types = TrashType.values();
        for (int i = 0; i < types.length; i++) {
            double xPos = i * binWidth;

            // --- DÒNG SỬA LỖI NẰM Ở ĐÂY ---
            // Thêm binWidth và binHeight vào constructor cho đúng
            TrashBin bin = new TrashBin(xPos, yPos, binWidth, binHeight, types[i]);

            trashBins.add(bin);
            root.getChildren().add(bin.getView());
        }
    }

    // <-- THAY ĐỔI: Phương thức này giờ nhận thông tin từ Server để tạo rác
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

    // <-- THAY ĐỔI: Cập nhật cả X và Y cho đối thủ
    public void updateOpponentPosition(String username, double x, double y) {
        if (player2 != null && player2.getUsername().equals(username)) {
            player2.setPosition(x, y);
        }
    }

    // <-- THÊM: Các phương thức helper được gọi từ Client.java
    public void updatePlayerScore(String username, int score) {
        Player player = getPlayerByName(username);
        if (player != null) {
            player.setScore(score);
            updateScores(); // Cập nhật UI ngay sau khi set điểm
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

    // <-- THÊM: Phương thức hiển thị màn hình Game Over
    public void showGameOver(String winnerName) {
        stopGameLoop(); // Dừng vòng lặp game

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
        backToMenuButton.setFont(Font.font("Arial", 20));
        backToMenuButton.setOnAction(e -> Main.getInstance().showMenuScene());

        gameOverPane.getChildren().addAll(gameOverLabel, winnerLabel, backToMenuButton);

        // Căn giữa panel trên màn hình
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