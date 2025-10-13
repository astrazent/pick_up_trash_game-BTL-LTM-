package gamePlay.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

public class InputHandler {
    private final Set<KeyCode> activeKeys = new HashSet<>();

    public InputHandler(Scene scene) {
        scene.setOnKeyPressed(event -> activeKeys.add(event.getCode()));
        scene.setOnKeyReleased(event -> activeKeys.remove(event.getCode()));
    }

    public boolean isKeyPressed(KeyCode keyCode) {
        return activeKeys.contains(keyCode);
    }

    public void clearKeys() {
        activeKeys.clear();
    }
}