package client.game;

import java.io.File;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class TrashBin extends GameObject {
    private final TrashType binType;

    public TrashBin(double x, double y, double width, double height, TrashType binType) {
        super(x, y, width, height, true); // Không tạo Rectangle mặc định
        this.binType = binType;

        // Tạo ImageView cho thùng rác
        createBinImage();
    }
    
    private void createBinImage() {
        try {
            String imagePath = getBinImagePath(binType);
            if (imagePath != null && new File(imagePath).exists()) {
                Image binImage = new Image(new File(imagePath).toURI().toString());
                ImageView imageView = new ImageView(binImage);
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(false);
                this.view = imageView;
                render();
            } else {
                // Fallback về Rectangle nếu không tìm thấy ảnh
                createRectangleFallback();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi load ảnh thùng rác: " + e.getMessage());
            createRectangleFallback();
        }
    }
    
    private void createRectangleFallback() {
        Rectangle binRect = new Rectangle(0, 0, width, height);
        binRect.setFill(binType.getColor().darker());
        binRect.setStroke(Color.BLACK);
        this.view = binRect;
        render();
    }
    
    private String getBinImagePath(TrashType type) {
        // Map TrashType to bin image
        switch (type) {
            case METAL:
                return "assets/images/bins/red.png";
            case ORGANIC:
                return "assets/images/bins/green.png";
            case PAPER:
                return "assets/images/bins/blue.png";
            case PLASTIC:
                return "assets/images/bins/orange.png";
            default:
                return null;
        }
    }

    @Override
    public void update() {
        // Thùng rác đứng yên, không cần update logic
    }
    
    // Phương thức để cập nhật vị trí và kích thước khi resize
    public void updatePosition(double newX, double newY, double newWidth, double newHeight) {
        this.x = newX;
        this.y = newY;
        this.width = newWidth;
        this.height = newHeight;
        
        // Cập nhật kích thước của view
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            imageView.setFitWidth(newWidth);
            imageView.setFitHeight(newHeight);
        } else if (view instanceof Rectangle) {
            Rectangle rect = (Rectangle) view;
            rect.setWidth(newWidth);
            rect.setHeight(newHeight);
        }
        
        render();
    }

    public TrashType getBinType() {
        return binType;
    }
}