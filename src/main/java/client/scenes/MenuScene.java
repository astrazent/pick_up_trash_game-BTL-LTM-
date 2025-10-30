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
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import client.data.UserProfile;


public class MenuScene {
    private Scene scene;
    private TableView<UserProfile> onlineTable;
    private TableColumn<UserProfile, String> usernameColumn;
    private TableColumn<UserProfile, String> statusColumn;
    private TableColumn<UserProfile, Integer> scoreColumn;

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

        // Bảng danh sách người chơi
        onlineTable = new TableView<>();

        usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setPrefWidth(120);

        statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> {
            String username = cellData.getValue().getUsername();
            boolean isOnline = Client.getSavedOnlineUsersStatic() != null &&
                    Client.getSavedOnlineUsersStatic().contains(username);
            return new SimpleStringProperty(isOnline ? "Online" : "Offline");
        });
        statusColumn.setPrefWidth(60);

        scoreColumn = new TableColumn<>("High Score");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreColumn.setPrefWidth(80);

        // Căn giữa nội dung của cột Status
        // Hiển thị chấm tròn thay vì chữ Online/Offline
        statusColumn.setCellFactory(column -> new javafx.scene.control.TableCell<UserProfile, String>() {
            private final javafx.scene.shape.Circle statusCircle = new javafx.scene.shape.Circle(6); // kích thước chấm tròn

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Chọn màu dựa vào trạng thái
                    if (item.equalsIgnoreCase("Online")) {
                        statusCircle.setFill(javafx.scene.paint.Color.LIMEGREEN);
                    } else {
                        statusCircle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                    }

                    setGraphic(statusCircle);   // gán chấm tròn làm nội dung ô
                    setAlignment(Pos.CENTER);   // căn giữa
                    setText(null);              // không hiển thị text
                }
            }
        });


        // Căn giữa nội dung của cột Score
        scoreColumn.setCellFactory(column -> new javafx.scene.control.TableCell<UserProfile, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setAlignment(Pos.CENTER); // căn giữa nội dung trong cell
                }
            }
        });

        onlineTable.getColumns().addAll(usernameColumn, statusColumn, scoreColumn);
        onlineTable.setPrefHeight(150);
        onlineTable.setMaxWidth(262);
        onlineTable.setPlaceholder(new Text("No players available"));

        layout.getChildren().addAll(title, onePlayerBtn, twoPlayersBtn, leaderboardBtn, historyBtn, onlineTable);
        scene = new Scene(layout, 400, 500);
    }

    public Scene getScene() {
        return scene;
    }

    public void updateOnlineList(java.util.List<UserProfile> allUsers) {
        ObservableList<UserProfile> list = FXCollections.observableArrayList(allUsers);
        onlineTable.setItems(list);
    }

}