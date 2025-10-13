package gamePlay.scenes;

import gamePlay.Main;
import gamePlay.config.GameConfig;
import gamePlay.game.GameLoop;
import gamePlay.game.Player;
import gamePlay.game.Trash;
import gamePlay.game.TrashBin; // <-- IMPORT
import gamePlay.game.TrashType; // <-- IMPORT
import gamePlay.input.InputHandler;
import gamePlay.network.Client;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public class GameScene {
    private final Scene scene;
    private final Pane root;
    private GameLoop gameLoop;

    private Player player1;
    private Player player2;
    private final List<Trash> trashList = new ArrayList<>();
    private final List<TrashBin> trashBins = new ArrayList<>(); // <-- THÊM DANH SÁCH THÙNG RÁC

    private final Label scoreLabel1;
    private final Label scoreLabel2;

    public GameScene(int playerCount, String p1Name, String p2Name) {
        root = new Pane();
        GameConfig config = Main.getInstance().getGameConfig();
        scene = new Scene(root, config.window.width, config.window.height);

        // --- UI Elements ---
        scoreLabel1 = new Label("P1 Score: 0");
        scoreLabel1.setFont(Font.font(18));
        scoreLabel1.setTextFill(Color.WHITE);
        scoreLabel1.setTranslateX(10);
        scoreLabel1.setTranslateY(10);

        scoreLabel2 = new Label("P2 Score: 0");
        scoreLabel2.setFont(Font.font(18));
        scoreLabel2.setTextFill(Color.WHITE);
        scoreLabel2.setTranslateX(config.window.width - 150);
        scoreLabel2.setTranslateY(10);

        root.setStyle("-fx-background-color: #333;");
        root.getChildren().add(scoreLabel1);

        // --- Game Objects ---
        setupPlayers(playerCount, config, p1Name, p2Name);
        setupTrashBins(config); // <-- GỌI HÀM KHỞI TẠO THÙNG RÁC

        for (int i = 0; i < 5; i++) {
            spawnTrash();
        }

        // --- Input and GameLoop ---
        InputHandler inputHandler = new InputHandler(scene);
        // TRUYỀN DANH SÁCH THÙNG RÁC VÀO GAMELOOP
        gameLoop = new GameLoop(this, inputHandler, player1, player2, trashList, trashBins);
    }

    private void setupPlayers(int playerCount, GameConfig config, String p1Name, String p2Name) {
        String myUsername = Client.getInstance().getUsername();
        player1 = new Player(config.window.width / 2.0 - 50, config.window.height - config.player.height - 10, myUsername);
        root.getChildren().add(player1.getView());

        if (playerCount == 2) {
            String opponentName = myUsername.equals(p1Name) ? p2Name : p1Name;
            player2 = new Player(config.window.width / 2.0 + 50, config.window.height - config.player.height - 10, opponentName);
            if (player2.getView() instanceof Rectangle) {
                ((Rectangle) player2.getView()).setFill(Color.LIGHTGREEN);
            }
            root.getChildren().add(player2.getView());
            root.getChildren().add(scoreLabel2);
        }
    }

    // --- HÀM MỚI ĐỂ TẠO THÙNG RÁC ---
    private void setupTrashBins(GameConfig config) {
        double binWidth = config.window.width / 4.0;
        double binHeight = 60;
        double yPos = config.window.height - binHeight;

        TrashType[] types = TrashType.values();
        for (int i = 0; i < types.length; i++) {
            double xPos = i * binWidth;
            TrashBin bin = new TrashBin(xPos, yPos, binWidth, binHeight, types[i]);
            trashBins.add(bin);
            root.getChildren().add(bin.getView());
        }
    }


    public void spawnTrash() {
        Trash newTrash = new Trash();
        trashList.add(newTrash);
        root.getChildren().add(newTrash.getView());
    }

    public void updateScores() {
        scoreLabel1.setText(String.format("%s: %d", player1.getUsername(), player1.getScore()));
        if (player2 != null) {
            scoreLabel2.setText(String.format("%s: %d", player2.getUsername(), player2.getScore()));
        }
    }

    public void updateOpponentPosition(String username, double x) {
        if (player2 != null && player2.getUsername().equals(username)) {
            player2.setPosition(x);
        }
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