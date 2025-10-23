package client.scenes;

import client.Main;
import client.network.Client;
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
            // 1. Gửi yêu cầu lấy dữ liệu bảng xếp hạng lên server
            System.out.println("Gửi yêu cầu GET_LEADERBOARD tới server...");
            Client.getInstance().sendMessage("GET_LEADERBOARD");

            // 2. Chuyển sang màn hình bảng xếp hạng
            // Màn hình này sẽ được cập nhật khi server gửi dữ liệu về
            Main.getInstance().showLeaderboardScene();
        });

        Button historyBtn = new Button("Match History");
        historyBtn.setPrefSize(150, 40);
        historyBtn.setOnAction(e -> {
            // 1. Gửi yêu cầu lấy lịch sử đấu lên server
            System.out.println("Gửi yêu cầu GET_HISTORY tới server...");
            Client.getInstance().sendMessage("GET_HISTORY");

            // 2. Chuyển sang màn hình lịch sử
            Main.getInstance().showHistoryScene();
        });

        layout.getChildren().addAll(title, onePlayerBtn, twoPlayersBtn, leaderboardBtn, historyBtn);
        scene = new Scene(layout, 400, 300);
    }

    public Scene getScene() {
        return scene;
    }
}