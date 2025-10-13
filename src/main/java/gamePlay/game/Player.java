package gamePlay.game;

import gamePlay.Main;
import gamePlay.config.GameConfig;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player extends GameObject {
    private final int speed;
    private int score = 0;
    private String username;
    private Trash heldTrash = null;

    public Player(double x, double y, String username) {
        super(x, y, 0, 0);

        GameConfig.PlayerConfig config = Main.getInstance().getGameConfig().player;
        this.width = config.width;
        this.height = config.height;
        this.speed = config.speed;
        this.username = username;

        if (this.view instanceof Rectangle) {
            Rectangle playerRect = (Rectangle) this.view;
            playerRect.setWidth(this.width);
            playerRect.setHeight(this.height);
            playerRect.setFill(Color.ROYALBLUE);
        }
    }

    @Override
    public void update() {
        if (heldTrash != null) {
            heldTrash.x = this.x + (this.width / 2) - (heldTrash.width / 2);
            heldTrash.y = this.y - heldTrash.height - 5;
            heldTrash.render();
        }
        render();
    }

    // --- CÁC PHƯƠNG THỨC DI CHUYỂN ĐƯỢC CẬP NHẬT ---

    public void moveLeft() {
        x -= speed;
        if (x < 0) {
            x = 0;
        }
    }

    public void moveRight(double screenWidth) {
        x += speed;
        if (x > screenWidth - width) {
            x = screenWidth - width;
        }
    }

    public void moveUp() {
        y -= speed;
        // Giới hạn trên, ví dụ không cho đi cao hơn nửa màn hình
        double topBoundary = Main.getInstance().getGameConfig().window.height / 2.0;
        if (y < topBoundary) {
            y = topBoundary;
        }
    }

    public void moveDown(double screenHeight) {
        y += speed;
        if (y > screenHeight - height) {
            y = screenHeight - height;
        }
    }

    // --- PHẦN CÒN LẠI GIỮ NGUYÊN ---

    public void setPosition(double x) {
        this.x = x;
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        this.score++;
    }

    public void decrementScore() {
        this.score--;
    }

    public String getUsername() {
        return username;
    }

    public void pickUpTrash(Trash trash) {
        if (this.heldTrash == null) {
            this.heldTrash = trash;
        }
    }

    public void dropTrash() {
        this.heldTrash = null;
    }

    public boolean isHoldingTrash() {
        return this.heldTrash != null;
    }

    public boolean isHoldingTrash(Trash trash) {
        return this.heldTrash == trash;
    }

    public Trash getHeldTrash() {
        return heldTrash;
    }
}