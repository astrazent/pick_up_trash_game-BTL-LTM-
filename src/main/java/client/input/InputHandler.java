package client.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashSet;
import java.util.Set;

public class InputHandler {
    private final Set<KeyCode> activeKeys = new HashSet<>();

    public InputHandler(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> activeKeys.add(event.getCode()));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> activeKeys.remove(event.getCode()));
    }

    public boolean isKeyPressed(KeyCode keyCode) {
        return activeKeys.contains(keyCode);
    }

    public void clearKeys() {
        activeKeys.clear();
    }
}