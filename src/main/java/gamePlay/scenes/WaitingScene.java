package gamePlay.scenes;

import gamePlay.Main;
import gamePlay.network.Client;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class WaitingScene {
    private Scene scene;

    public WaitingScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        Label waitingLabel = new Label("Waiting for another player...");
        waitingLabel.setFont(Font.font(20));

        // Animation chấm chấm cho đẹp
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> waitingLabel.setText("Waiting for another player.")),
                new KeyFrame(Duration.seconds(1.0), e -> waitingLabel.setText("Waiting for another player..")),
                new KeyFrame(Duration.seconds(1.5), e -> waitingLabel.setText("Waiting for another player..."))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        layout.getChildren().add(waitingLabel);

        // Gửi trạng thái sẵn sàng lên server
        String username = Client.getInstance().getUsername();
        if (username != null) {
            Client.getInstance().sendMessage("READY;" + username);
        } else {
            // Xử lý lỗi nếu không có username (về lại màn login)
            Main.getInstance().showLoginScene();
        }

        scene = new Scene(layout, 400, 300);
    }

    public Scene getScene() {
        return scene;
    }
}