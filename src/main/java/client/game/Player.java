package client.game;

import java.io.File;

import client.Main;
import client.config.GameConfig;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player extends GameObject {
    private final int speed;
    private int score = 0;
    private String username;
    private Trash heldTrash = null;
    private int playerNumber; // 1 hoặc 2

    public Player(double x, double y, String username, int playerNumber) {
        super(x, y, 0, 0, true); // Không tạo Rectangle mặc định

        GameConfig.PlayerConfig config = Main.getInstance().getGameConfig().player;
        this.width = config.width;
        this.height = config.height;
        this.speed = config.speed;
        this.username = username;
        this.playerNumber = playerNumber;

        // Tạo ImageView cho player
        createPlayerImage();
    }
    
    private void createPlayerImage() {
        try {
            String imagePath = "assets/images/players/player" + playerNumber + ".png";
            File imageFile = new File(imagePath);
            
            if (imageFile.exists()) {
                Image playerImage = new Image(imageFile.toURI().toString());
                ImageView imageView = new ImageView(playerImage);
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(true);
                this.view = imageView;
                render();
            } else {
                // Fallback về Rectangle nếu không tìm thấy ảnh
                createRectangleFallback();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi load ảnh player: " + e.getMessage());
            createRectangleFallback();
        }
    }
    
    private void createRectangleFallback() {
        Rectangle playerRect = new Rectangle(0, 0, width, height);
        if (playerNumber == 1) {
            playerRect.setFill(Color.ROYALBLUE);
        } else {
            playerRect.setFill(Color.LIGHTGREEN);
        }
        this.view = playerRect;
        render();
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
        double topBoundary = 0;
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

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
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
