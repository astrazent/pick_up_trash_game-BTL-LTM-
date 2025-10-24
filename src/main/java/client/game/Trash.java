package client.game;

import client.Main;
import client.config.GameConfig;
import javafx.scene.shape.Rectangle;

import java.util.Random;

public class Trash extends GameObject {
    private int id;
    private int fallSpeed;
    private TrashType type;
    private static final Random random = new Random();

    public Trash(int id, double x, double y, TrashType type) {
        super(x, y, 0, 0);
        this.id = id;
        this.type = type;

        GameConfig.TrashConfig config = Main.getInstance().getGameConfig().trash;
        this.width = config.width;
        this.height = config.height;
        this.fallSpeed = config.fall_speed;

        if (this.view instanceof Rectangle) {
            Rectangle trashRect = (Rectangle) this.view;
            trashRect.setWidth(this.width);
            trashRect.setHeight(this.height);
            trashRect.setFill(type.getColor());
        }
    }

    public Trash() {
        super(0, 0, 0, 0);
        GameConfig.TrashConfig config = Main.getInstance().getGameConfig().trash;
        this.width = config.width;
        this.height = config.height;
        this.fallSpeed = config.fall_speed;
        resetPosition(Main.getInstance().getGameConfig().window.width);
    }

    @Override
    public void update() {
        y += fallSpeed;
        render();
    }

    public void updateState(double newX, double newY, TrashType newType) {
        this.x = newX;
        this.y = newY;
        this.type = newType;
        if (this.view instanceof Rectangle) {
            ((Rectangle) this.view).setFill(this.type.getColor());
        }
        render();
    }

    public void resetPosition(double screenWidth) {
        this.type = TrashType.values()[random.nextInt(TrashType.values().length)];
        if (this.view instanceof Rectangle) {
            ((Rectangle) this.view).setFill(this.type.getColor());
        }
        this.y = -height - random.nextInt(300);
        this.x = random.nextDouble() * (screenWidth - width);
        render();
    }

    public int getId() {
        return id;
    }

    public TrashType getType() {
        return type;
    }
}
