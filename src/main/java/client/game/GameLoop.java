package client.game;

import client.Main;
import client.config.GameConfig;
import client.input.InputHandler;
import client.network.Client;
import client.scenes.GameScene;
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
    private boolean isPaused = false;
    private final double screenWidth;
    private final double screenHeight;

    public GameLoop(GameScene gameScene, InputHandler inputHandler, Player player1, Player player2, List<Trash> trashList, List<TrashBin> trashBins) {
        this.gameScene = gameScene;
        this.inputHandler = inputHandler;
        this.player1 = player1;
        this.player2 = player2;
        this.trashList = trashList;
        this.trashBins = trashBins;

        GameConfig config = Main.getInstance().getGameConfig();
        this.screenWidth = config.window.width;
        this.screenHeight = config.window.height;
    }

    @Override
    public void handle(long now) {
        if (isPaused) return;
        handlePlayerMovement();

        if (player2 != null) {
            String posMessage = String.format("UPDATE_POS;%s;%f;%f", player1.getUsername(), player1.getX(), player1.getY());
            Client.getInstance().sendUDPMessage(posMessage);
        }

        player1.update();
        if (player2 != null) {
            player2.update();
        }

        for (Trash trash : trashList) {
            if (!player1.isHoldingTrash(trash) && (player2 == null || !player2.isHoldingTrash(trash))) {
                trash.update();
            }
        }

        handlePlayerActions();
    }
    public void pauseGame() {
        isPaused = true;
        this.stop(); // dừng AnimationTimer
    }

    public void resumeGame() {
        isPaused = false;
        this.start(); // tiếp tục AnimationTimer
    }
    public boolean isPaused() {
        return isPaused;
    }

    private void handlePlayerMovement() {
        if (inputHandler.isKeyPressed(KeyCode.A) || inputHandler.isKeyPressed(KeyCode.LEFT)) {
            player1.moveLeft();
        }
        if (inputHandler.isKeyPressed(KeyCode.D) || inputHandler.isKeyPressed(KeyCode.RIGHT)) {
            player1.moveRight(screenWidth);
        }
        if (inputHandler.isKeyPressed(KeyCode.W) || inputHandler.isKeyPressed(KeyCode.UP)) {
            player1.moveUp();
        }
        if (inputHandler.isKeyPressed(KeyCode.S) || inputHandler.isKeyPressed(KeyCode.DOWN)) {
            player1.moveDown(screenHeight);
        }
    }

    private void handlePlayerActions() {
        if (!player1.isHoldingTrash()) {
            for (Trash trash : trashList) {
                if (player1.checkCollision(trash)) {
                    String message = String.format("PICK_TRASH;%s;%d", player1.getUsername(), trash.getId());
                    Client.getInstance().sendMessage(message);
                    break;
                }
            }
        } else if (inputHandler.isKeyPressed(KeyCode.SPACE)) {
            boolean onBin = false;
            for (TrashBin bin : trashBins) {
                if (player1.checkCollision(bin)) {
                    String message = String.format("DROP_TRASH;%s;%s", player1.getUsername(), bin.getBinType().name());
                    System.out.println(Client.getInstance().getUsername());
                    System.out.println("check gameLoop: "+message);
                    Client.getInstance().sendMessage(message);
                    onBin = true;
                    break;
                }
            }
        }
    }
}
