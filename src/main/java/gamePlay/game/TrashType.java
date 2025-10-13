package gamePlay.game;

import javafx.scene.paint.Color;

public enum TrashType {
    ORGANIC(Color.BROWN),   // Hữu cơ
    PLASTIC(Color.YELLOW),  // Nhựa
    METAL(Color.GRAY),      // Kim loại
    PAPER(Color.WHITE);     // Giấy

    private final Color color;

    TrashType(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}