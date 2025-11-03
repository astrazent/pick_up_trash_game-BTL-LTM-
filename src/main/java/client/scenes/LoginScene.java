package client.scenes;

import client.network.Client;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class LoginScene {
    private Scene scene;
    private Text actionTarget;

    public LoginScene() {
        // Container chính với gradient background
        StackPane root = new StackPane();
        
        // Tạo gradient background xanh lá cây tươi mát
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#2ecc71")),
            new Stop(1, Color.web("#27ae60"))
        );
        BackgroundFill bgFill = new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY);
        root.setBackground(new Background(bgFill));

        // Panel đăng nhập với shadow
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40, 50, 40, 50));
        loginBox.setMaxWidth(420);
        loginBox.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );

        // Icon rác tái chế (emoji)
        Text icon = new Text("♻️");
        icon.setFont(Font.font(50));

        // Tiêu đề
        Text sceneTitle = new Text("Pick Up Trash Game");
        sceneTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        sceneTitle.setFill(Color.web("#27ae60"));

        Text subtitle = new Text("Clean the World, One Trash at a Time!");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subtitle.setFill(Color.web("#7f8c8d"));

        // Khoảng cách
        Region spacer1 = new Region();
        spacer1.setPrefHeight(10);

        // Username field
        VBox usernameBox = new VBox(8);
        Label userName = new Label("👤 Username");
        userName.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        userName.setTextFill(Color.web("#2c3e50"));
        
        TextField userTextField = new TextField();
        userTextField.setPromptText("Enter your username");
        userTextField.setText("player1");
        userTextField.setPrefHeight(40);
        userTextField.setStyle(
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-padding: 0 15 0 15; " +
            "-fx-font-size: 14;"
        );
        
        usernameBox.getChildren().addAll(userName, userTextField);

        // Password field
        VBox passwordBox = new VBox(8);
        Label pw = new Label("🔒 Password");
        pw.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        pw.setTextFill(Color.web("#2c3e50"));
        
        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter your password");
        pwBox.setText("123");
        pwBox.setPrefHeight(40);
        pwBox.setStyle(
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-padding: 0 15 0 15; " +
            "-fx-font-size: 14;"
        );
        
        passwordBox.getChildren().addAll(pw, pwBox);

        // Sign in button
        Button btn = new Button("SIGN IN");
        btn.setPrefHeight(45);
        btn.setPrefWidth(200);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setStyle(
            "-fx-background-color: #27ae60; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 25; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        );
        
        // Hover effect
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #2ecc71; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 25; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #27ae60; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 25; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        ));

        actionTarget = new Text();
        actionTarget.setFont(Font.font("Segoe UI", 12));
        actionTarget.setFill(Color.web("#e74c3c"));

        btn.setOnAction(e -> {
            actionTarget.setFill(Color.web("#f39c12"));
            actionTarget.setText("⏳ Connecting...");
            String username = userTextField.getText();
            String password = pwBox.getText();
            Client.getInstance().sendMessage("LOGIN;" + username + ";" + password);
        });

        loginBox.getChildren().addAll(
            icon, sceneTitle, subtitle, spacer1,
            usernameBox, passwordBox, btn, actionTarget
        );

        root.getChildren().add(loginBox);
        scene = new Scene(root, 600, 500);
    }

    public Scene getScene() {
        return scene;
    }

    public void showError(String message) {
        actionTarget.setFill(Color.web("#e74c3c"));
        actionTarget.setText("❌ " + message);
    }
}