package gamePlay.scenes;

import gamePlay.Main;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class MenuScene {
    private Scene scene;

    public MenuScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Text title = new Text("Main Menu");
        title.setFont(Font.font(24));

        Button onePlayerBtn = new Button("1 Player Mode");
        onePlayerBtn.setPrefSize(150, 40);
        onePlayerBtn.setOnAction(e -> {
            Main.getInstance().showGameScene(1);
        });

        Button twoPlayersBtn = new Button("2 Players Mode");
        twoPlayersBtn.setPrefSize(150, 40);
        twoPlayersBtn.setOnAction(e -> {
            // Gửi yêu cầu sẵn sàng tới server và chuyển sang màn hình chờ
            Main.getInstance().showWaitingScene();
        });

        Button leaderboardBtn = new Button("Leaderboard");
        leaderboardBtn.setPrefSize(150, 40);
        leaderboardBtn.setOnAction(e -> {
            // Main.getInstance().showLeaderboardScene();
            System.out.println("Leaderboard feature is not implemented yet.");
        });

        layout.getChildren().addAll(title, onePlayerBtn, twoPlayersBtn, leaderboardBtn);
        scene = new Scene(layout, 400, 300);
    }

    public Scene getScene() {
        return scene;
    }
}