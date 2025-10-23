package client.config;

// Lớp này sử dụng các thuộc tính public để Jackson có thể dễ dàng map dữ liệu từ file YAML.
// Một cách tiếp cận khác là dùng private fields và các phương thức getter/setter.
public class GameConfig {
    public WindowConfig window;
    public PlayerConfig player;
    public TrashConfig trash;
    public GameSettings game;

    public static class WindowConfig {
        public String title;
        public int width;
        public int height;
    }

    public static class PlayerConfig {
        public int speed;
        public int width;
        public int height;
    }

    public static class TrashConfig {
        public int fall_speed;
        public int spawn_rate_ms;
        public int width;
        public int height;
    }

    public static class GameSettings {
        public int fps;
    }
}