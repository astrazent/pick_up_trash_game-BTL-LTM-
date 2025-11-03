package client.scenes;

import client.Main;
import client.network.Client;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class WaitingAcceptanceScene {
    private Scene scene;

    public WaitingAcceptanceScene(String opponentName) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        Label waitingLabel = new Label("Waiting for response from opponent...");
        waitingLabel.setFont(Font.font(20));

        // Animation chấm chấm cho đẹp
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> waitingLabel.setText("Waiting for response from opponent.")),
                new KeyFrame(Duration.seconds(1.0), e -> waitingLabel.setText("Waiting for response from opponent..")),
                new KeyFrame(Duration.seconds(1.5), e -> waitingLabel.setText("Waiting for response from opponent..."))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        layout.getChildren().add(waitingLabel);

        // Gửi trạng thái sẵn sàng lên server
        String username = Client.getInstance().getUsername();
        if (username != null) {
            Client.getInstance().sendMessage("CHALLENGE_REQUEST;" + username + ";" + opponentName);
        } else {
            // Xử lý lỗi nếu không có username (về lại màn login)
            Main.getInstance().showLoginScene();
        }

        scene = new Scene(layout, 400, 300);
    }

    public static void showAutoDeclinePopup() {
        javafx.application.Platform.runLater(() -> {
            // Nếu popup cũ còn mở thì đóng lại
            javafx.stage.Popup popup = new javafx.stage.Popup();

            javafx.scene.layout.VBox popupContent = new javafx.scene.layout.VBox(10);
            popupContent.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: gray; " +
                            "-fx-border-radius: 10; " +
                            "-fx-background-radius: 10; " +
                            "-fx-padding: 20; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 2);"
            );
            popupContent.setAlignment(Pos.CENTER);

            javafx.scene.text.Text line1 = new javafx.scene.text.Text("KHÔNG THỂ GỬI LỜI THÁCH ĐẤU!");
            line1.setFont(Font.font("Segoe UI", 16));
            line1.setFill(javafx.scene.paint.Color.web("#c0392b"));

            javafx.scene.text.Text line2 = new javafx.scene.text.Text("Người chơi này đang thách đấu hoặc\n");
            javafx.scene.text.Text line3 = new javafx.scene.text.Text("nhận được lời thách đấu từ người chơi khác");

            line2.setFont(Font.font("Segoe UI", 13));
            line2.setFill(javafx.scene.paint.Color.web("#c0392b"));
            line3.setFont(Font.font("Segoe UI", 13));
            line3.setFill(javafx.scene.paint.Color.web("#c0392b"));

            javafx.scene.text.TextFlow textFlow = new javafx.scene.text.TextFlow(line2, line3);
            textFlow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER); // căn giữa các dòng
            textFlow.setLineSpacing(2); // tuỳ chọn: khoảng cách giữa 2 dòng
            textFlow.setMaxWidth(300);  // giới hạn chiều rộng để tự xuống dòng


            javafx.scene.control.Button okButton = new javafx.scene.control.Button("OK");
            okButton.setStyle(
                    "-fx-background-color: #2ecc71; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-pref-width: 80;"
            );

            popupContent.getChildren().addAll(line1, textFlow, okButton);
            popup.getContent().add(popupContent);

            // Lấy cửa sổ hiện tại để căn giữa popup
            javafx.stage.Window window = Main.getPrimaryStage();
            double centerX = window.getX() + window.getWidth() / 2 - 150;
            double centerY = window.getY() + window.getHeight() / 2 - 100;
            popup.show(window, centerX, centerY);

            okButton.setOnAction(e -> {
                popup.hide();
                Main.getInstance().showMenuScene();
            });
        });
    }

    public Scene getScene() {
        return scene;
    }
}