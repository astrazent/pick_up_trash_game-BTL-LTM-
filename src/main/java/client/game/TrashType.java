package client.game;

import javafx.scene.paint.Color;

public enum TrashType {
    ORGANIC(Color.BROWN, "organics"),   // Hữu cơ
    PLASTIC(Color.YELLOW, "plastics"),  // Nhựa
    METAL(Color.GRAY, "metals"),        // Kim loại
    PAPER(Color.WHITE, "papers");       // Giấy

    private final Color color;
    private final String folderName;

    TrashType(Color color, String folderName) {
        this.color = color;
        this.folderName = folderName;
    }

    public Color getColor() {
        return color;
    }
    
    public String getFolderName() {
        return folderName;
    }
}