package client.game;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import client.Main;
import client.config.GameConfig;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

public class Trash extends GameObject {
    private int id;
    private final int fallSpeed;
    private TrashType type;
    private int imageIndex; // Index của ảnh được server chỉ định
    private static final Random random = new Random();
    private static final List<String> metalImages = new ArrayList<>();
    private static final List<String> organicImages = new ArrayList<>();
    private static final List<String> paperImages = new ArrayList<>();
    private static final List<String> plasticImages = new ArrayList<>();
    
    static {
        // Load danh sách tất cả các file ảnh trash
        loadTrashImages("assets/images/trash/metals", metalImages);
        loadTrashImages("assets/images/trash/organics", organicImages);
        loadTrashImages("assets/images/trash/papers", paperImages);
        loadTrashImages("assets/images/trash/plastics", plasticImages);
    }
    
    private static void loadTrashImages(String folderPath, List<String> imageList) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    imageList.add(file.getPath());
                }
            }
        }
    }

    public Trash(int id, double x, double y, TrashType type, int imageIndex) {
        super(x, y, 0, 0, true); // Không tạo Rectangle mặc định
        this.id = id;
        this.type = type;
        this.imageIndex = imageIndex;

        GameConfig.TrashConfig config = Main.getInstance().getGameConfig().trash;
        this.width = config.width;
        this.height = config.height;
        this.fallSpeed = config.fall_speed;

        // Tạo ImageView cho trash với imageIndex cụ thể
        createTrashImage();
    }

    public Trash() {
        super(0, 0, 0, 0, true); // Không tạo Rectangle mặc định
        GameConfig.TrashConfig config = Main.getInstance().getGameConfig().trash;
        this.width = config.width;
        this.height = config.height;
        this.fallSpeed = config.fall_speed;
        
        // Random type và imageIndex trước
        this.type = TrashType.values()[random.nextInt(TrashType.values().length)];
        this.imageIndex = random.nextInt(getImageCountForType(this.type));
        
        // Tạo ImageView
        createTrashImage();
        
        resetPosition(Main.getInstance().getGameConfig().window.width);
    }
    
    private void createTrashImage() {
        try {
            String imagePath = getImagePathByIndex(type, imageIndex);
            if (imagePath != null && new File(imagePath).exists()) {
                Image trashImage = new Image(new File(imagePath).toURI().toString());
                ImageView imageView = new ImageView(trashImage);
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
            System.err.println("Lỗi khi load ảnh trash: " + e.getMessage());
            createRectangleFallback();
        }
    }
    
    private void createRectangleFallback() {
        Rectangle rect = new Rectangle(0, 0, width, height);
        rect.setFill(type.getColor());
        this.view = rect;
        render();
    }
    
    private String getImagePathByIndex(TrashType type, int index) {
        List<String> images;
        switch (type) {
            case METAL:
                images = metalImages;
                break;
            case ORGANIC:
                images = organicImages;
                break;
            case PAPER:
                images = paperImages;
                break;
            case PLASTIC:
                images = plasticImages;
                break;
            default:
                return null;
        }
        
        if (images.isEmpty() || index < 0 || index >= images.size()) {
            return null;
        }
        
        return images.get(index);
    }

    @Override
    public void update() {
        y += fallSpeed;
        render();
    }

    public void updateState(double newX, double newY, TrashType newType, int newImageIndex) {
        this.x = newX;
        this.y = newY;
        
        // Chỉ thay đổi hình ảnh nếu type hoặc imageIndex thay đổi
        if (this.type != newType || this.imageIndex != newImageIndex) {
            this.type = newType;
            this.imageIndex = newImageIndex;
            createTrashImage();
        }
        
        render();
    }

    public void resetPosition(double screenWidth) {
        TrashType newType = TrashType.values()[random.nextInt(TrashType.values().length)];
        int newImageIndex = random.nextInt(getImageCountForType(newType));
        
        // Chỉ thay đổi hình ảnh nếu type hoặc imageIndex thay đổi
        if (this.type != newType || this.imageIndex != newImageIndex) {
            this.type = newType;
            this.imageIndex = newImageIndex;
            createTrashImage();
        }
        
        this.y = -height - random.nextInt(300);
        this.x = random.nextDouble() * (screenWidth - width);
        render();
    }
    
    private int getImageCountForType(TrashType type) {
        int count;
        switch (type) {
            case METAL:
                count = metalImages.size();
                break;
            case ORGANIC:
                count = organicImages.size();
                break;
            case PAPER:
                count = paperImages.size();
                break;
            case PLASTIC:
                count = plasticImages.size();
                break;
            default:
                count = 0;
        }
        // Đảm bảo trả về ít nhất 1 để tránh lỗi random.nextInt(0)
        return Math.max(1, count);
    }

    public int getId() {
        return id;
    }

    public TrashType getType() {
        return type;
    }
}
