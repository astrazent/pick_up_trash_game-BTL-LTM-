package client.game;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

public abstract class GameObject {
    protected Node view; // Giao diện của đối tượng (có thể là Rectangle, ImageView,...)
    protected double x, y;
    protected double width, height;

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // --- THAY ĐỔI QUAN TRỌNG ---
        // Tạo Rectangle tại (0,0) thay vì (x,y)
        this.view = new Rectangle(0, 0, width, height);

        // Đặt vị trí ban đầu bằng cách dịch chuyển nó
        render();
    }
    
    // Constructor không tạo view mặc định (cho các lớp con tự tạo view)
    protected GameObject(double x, double y, double width, double height, boolean noDefaultView) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // Không tạo view, lớp con sẽ tự tạo
    }

    public abstract void update();

    // Cập nhật vị trí của Node trên màn hình
    protected void render() {
        view.setTranslateX(x);
        view.setTranslateY(y);
    }

    public Node getView() {
        return view;
    }

    public boolean checkCollision(GameObject other) {
        return this.getView().getBoundsInParent().intersects(other.getView().getBoundsInParent());
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    
    // Phương thức để set vị trí (public để có thể truy cập từ bên ngoài)
    public void setX(double x) {
        this.x = x;
        render();
    }
    
    public void setY(double y) {
        this.y = y;
        render();
    }
}