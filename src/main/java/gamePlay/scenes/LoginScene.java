package gamePlay.scenes;

import gamePlay.Main;
import gamePlay.network.Client;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class LoginScene {
    private Scene scene;
    private Text actionTarget;

    public LoginScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text sceneTitle = new Text("Welcome to Pick Up Trash!");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label userName = new Label("User Name:");
        grid.add(userName, 0, 1);
        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 1);
        userTextField.setText("player1"); // Giá trị mặc định để test nhanh

        Label pw = new Label("Password:");
        grid.add(pw, 0, 2);
        PasswordField pwBox = new PasswordField();
        grid.add(pwBox, 1, 2);
        pwBox.setText("123"); // Giá trị mặc định

        Button btn = new Button("Sign in");
        grid.add(btn, 1, 4);

        actionTarget = new Text();
        grid.add(actionTarget, 1, 6);

        btn.setOnAction(e -> {
            actionTarget.setFill(Color.FIREBRICK);
            actionTarget.setText("Connecting...");
            String username = userTextField.getText();
            String password = pwBox.getText();
            Client.getInstance().sendMessage("LOGIN;" + username + ";" + password);
        });

        scene = new Scene(grid, 400, 300);
    }

    public Scene getScene() {
        return scene;
    }

    public void showError(String message) {
        actionTarget.setText(message);
    }
}