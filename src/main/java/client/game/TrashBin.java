package client.game;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class TrashBin extends GameObject {
    private final TrashType binType;

    public TrashBin(double x, double y, double width, double height, TrashType binType) {
        super(x, y, width, height);
        this.binType = binType;

        if (this.view instanceof Rectangle) {
            Rectangle binRect = (Rectangle) this.view;
            binRect.setFill(binType.getColor().darker()); // Màu thùng rác sẽ tối hơn màu rác
            binRect.setStroke(Color.BLACK);
        }
    }

    @Override
    public void update() {
        // Thùng rác đứng yên, không cần update logic
    }

    public TrashType getBinType() {
        return binType;
    }
}