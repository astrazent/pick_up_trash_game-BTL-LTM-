package client.scenes;

import client.Main;
import client.network.Client;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class WaitingScene {
    private Scene scene;

    public WaitingScene() {
        // Container chính với gradient background
        StackPane root = new StackPane();
        
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#667eea")),
            new Stop(1, Color.web("#764ba2"))
        );
        BackgroundFill bgFill = new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY);
        root.setBackground(new Background(bgFill));

        VBox layout = new VBox(30);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.setMaxWidth(450);
        layout.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95); " +
            "-fx-background-radius: 20; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );

        // Icon loading spinner
        StackPane spinnerBox = new StackPane();
        Circle outerCircle = new Circle(40);
        outerCircle.setFill(Color.TRANSPARENT);
        outerCircle.setStroke(Color.web("#e0e0e0"));
        outerCircle.setStrokeWidth(4);

        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#667eea"));
        spinner.setStrokeWidth(4);
        spinner.getStrokeDashArray().addAll(80.0, 200.0);

        // Rotation animation
        RotateTransition rotate = new RotateTransition(Duration.seconds(2), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(RotateTransition.INDEFINITE);
        rotate.play();

        spinnerBox.getChildren().addAll(outerCircle, spinner);

        // Icon người chơi
        Text icon = new Text("👥");
        icon.setFont(Font.font(50));

        // Tiêu đề
        Text title = new Text("Waiting Room");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setFill(Color.web("#2c3e50"));

        // Label chờ với animation
        Label waitingLabel = new Label("Waiting for another player...");
        waitingLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        waitingLabel.setTextFill(Color.web("#7f8c8d"));
        waitingLabel.setWrapText(true);
        waitingLabel.setAlignment(Pos.CENTER);
        waitingLabel.setMaxWidth(350);

        // Animation chấm chấm
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> waitingLabel.setText("Waiting for another player.")),
                new KeyFrame(Duration.seconds(1.0), e -> waitingLabel.setText("Waiting for another player..")),
                new KeyFrame(Duration.seconds(1.5), e -> waitingLabel.setText("Waiting for another player..."))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Thông tin trạng thái
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(15));
        statusBox.setStyle(
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 10;"
        );

        Text statusIcon = new Text("✓");
        statusIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        statusIcon.setFill(Color.web("#27ae60"));

        Text statusText = new Text("You are ready!");
        statusText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        statusText.setFill(Color.web("#2c3e50"));

        statusBox.getChildren().addAll(statusIcon, statusText);

        // Mẹo chơi game
        VBox tipBox = new VBox(8);
        tipBox.setAlignment(Pos.CENTER);
        tipBox.setPadding(new Insets(15));
        tipBox.setStyle(
            "-fx-background-color: #fff3cd; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: #ffc107; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 2;"
        );

        Text tipTitle = new Text("💡 Tip");
        tipTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        tipTitle.setFill(Color.web("#856404"));

        Text tipText = new Text("Sort trash into correct bins to earn points!");
        tipText.setFont(Font.font("Segoe UI", 12));
        tipText.setFill(Color.web("#856404"));
        tipText.setWrappingWidth(300);
        tipText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        tipBox.getChildren().addAll(tipTitle, tipText);

        // Nút Cancel
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        cancelBtn.setPrefSize(150, 45);
        cancelBtn.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 10; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);"
        );

        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
            "-fx-background-color: #c0392b; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 10; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);"
        ));

        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 10; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);"
        ));

        cancelBtn.setOnAction(e -> {
            // Gửi message hủy chờ lên server
            String username = Client.getInstance().getUsername();
            if (username != null) {
                Client.getInstance().sendMessage("CANCEL_WAITING;" + username);
            }
            // Quay lại menu ngay lập tức
            Main.getInstance().showMenuScene();
        });

        layout.getChildren().addAll(spinnerBox, icon, title, waitingLabel, statusBox, tipBox, cancelBtn);

        // Gửi trạng thái sẵn sàng lên server
        String username = Client.getInstance().getUsername();
        if (username != null) {
            Client.getInstance().sendMessage("READY;" + username);
        } else {
            Main.getInstance().showLoginScene();
        }

        root.getChildren().add(layout);
        scene = new Scene(root, 550, 650);
    }

    public Scene getScene() {
        return scene;
    }
}