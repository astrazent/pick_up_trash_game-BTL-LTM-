package gamePlay.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gamePlay.config.GameConfig;
import gamePlay.config.NetworkConfig;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.Objects;

public class ResourceLoader {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    // Tải cấu hình game từ file game.yml
    public static GameConfig loadGameConfig() {
        try (InputStream is = getResourceAsStream("config/game.yml")) {
            return yamlMapper.readValue(is, GameConfig.class);
        } catch (Exception e) {
            System.err.println("Không thể tải file cấu hình game.yml");
            e.printStackTrace();
            return null; // Hoặc ném ra một exception để dừng chương trình
        }
    }

    // Tải cấu hình mạng từ file network.yml
    public static NetworkConfig loadNetworkConfig() {
        try (InputStream is = getResourceAsStream("config/network.yml")) {
            return yamlMapper.readValue(is, NetworkConfig.class);
        } catch (Exception e) {
            System.err.println("Không thể tải file cấu hình network.yml");
            e.printStackTrace();
            return null;
        }
    }

    // Tải hình ảnh từ thư mục resources/images
    public static Image loadImage(String name) {
        try (InputStream is = getResourceAsStream("images/" + name)) {
            return new Image(is);
        } catch (Exception e) {
            System.err.println("Không thể tải hình ảnh: " + name);
            return null;
        }
    }

    // Phương thức trợ giúp để lấy tài nguyên
    private static InputStream getResourceAsStream(String resourceName) {
        return Objects.requireNonNull(
                ResourceLoader.class.getClassLoader().getResourceAsStream(resourceName),
                "Tài nguyên không tìm thấy: " + resourceName
        );
    }
}