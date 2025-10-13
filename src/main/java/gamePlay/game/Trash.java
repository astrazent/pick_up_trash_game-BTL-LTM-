package gamePlay.game;

import gamePlay.Main;
import gamePlay.config.GameConfig;
import javafx.scene.shape.Rectangle;
import java.util.Random;

public class Trash extends GameObject {
    private int fallSpeed;
    private TrashType type; // <-- THÊM LOẠI RÁC
    private static final Random random = new Random();

    public Trash() {
        super(0, 0, 0, 0);
        GameConfig.TrashConfig config = Main.getInstance().getGameConfig().trash;
        this.width = config.width;
        this.height = config.height;
        this.fallSpeed = config.fall_speed;

        if (this.view instanceof Rectangle) {
            Rectangle trashRect = (Rectangle) this.view;
            trashRect.setWidth(this.width);
            trashRect.setHeight(this.height);
        }

        resetPosition(Main.getInstance().getGameConfig().window.width);
    }

    @Override
    public void update() {
        y += fallSpeed;
        render();
    }

    public void resetPosition(double screenWidth) {
        // Gán một loại ngẫu nhiên khi reset
        this.type = TrashType.values()[random.nextInt(TrashType.values().length)];

        // Cập nhật màu sắc theo loại
        if (this.view instanceof Rectangle) {
            ((Rectangle) this.view).setFill(this.type.getColor());
        }

        this.y = -height - random.nextInt(300);
        this.x = random.nextDouble() * (screenWidth - width);
        render();
    }

    public TrashType getType() {
        return type;
    }
}