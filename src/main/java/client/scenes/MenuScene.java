package client.scenes;

import client.Main;
import client.data.UserProfile;
import client.network.Client;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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


public class MenuScene {
    private Scene scene;
    private TableView<UserProfile> onlineTable;
    private TableColumn<UserProfile, String> usernameColumn;
    private TableColumn<UserProfile, String> statusColumn;
    private TableColumn<UserProfile, Integer> scoreColumn;

    public MenuScene() {
        // Container chính với gradient background
        StackPane root = new StackPane();
        
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#3498db")),
            new Stop(1, Color.web("#2c3e50"))
        );
        BackgroundFill bgFill = new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY);
        root.setBackground(new Background(bgFill));

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setMaxWidth(500);
        layout.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95); " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );

        // Icon và tiêu đề
        Text icon = new Text("🎮");
        icon.setFont(Font.font(40));
        
        Text title = new Text("MAIN MENU");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setFill(Color.web("#2c3e50"));

        // --- HIỂN THỊ THÔNG TIN NGƯỜI DÙNG ---
        Client client = Client.getInstance();
        String username = client.getUsername();
        int highScore = client.getHighScore();

        HBox userInfoBox = new HBox(10);
        userInfoBox.setAlignment(Pos.CENTER);
        userInfoBox.setPadding(new Insets(15));
        userInfoBox.setStyle(
            "-fx-background-color: linear-gradient(to right, #16a085, #2ecc71); " +
            "-fx-background-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        );

        Text userInfo = new Text("👤 " + username);
        userInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        userInfo.setFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Text scoreInfo = new Text("🏆 " + highScore);
        scoreInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        scoreInfo.setFill(Color.web("#f39c12"));

        userInfoBox.getChildren().addAll(userInfo, spacer, scoreInfo);

        // Khoảng cách
        Region spacer1 = new Region();
        spacer1.setPrefHeight(5);

        // Container cho các nút
        VBox buttonsBox = new VBox(12);
        buttonsBox.setAlignment(Pos.CENTER);

        Button onePlayerBtn = createStyledButton("🎯 1 Player Mode", "#27ae60", "#2ecc71");
        onePlayerBtn.setOnAction(e -> Main.getInstance().showGameScene(1));

        Button twoPlayersBtn = createStyledButton("👥 2 Players Mode", "#e67e22", "#f39c12");
        twoPlayersBtn.setOnAction(e -> Main.getInstance().showWaitingScene());

        Button leaderboardBtn = createStyledButton("🏆 Leaderboard", "#8e44ad", "#9b59b6");
        leaderboardBtn.setOnAction(e -> {
            System.out.println("Gửi yêu cầu GET_LEADERBOARD tới server...");
            Client.getInstance().sendMessage("GET_LEADERBOARD");
            Main.getInstance().showLeaderboardScene();
        });

        Button historyBtn = createStyledButton("📜 Match History", "#2980b9", "#3498db");
        historyBtn.setOnAction(e -> {
            System.out.println("Gửi yêu cầu GET_HISTORY tới server...");
            Client.getInstance().sendMessage("GET_HISTORY");
            Main.getInstance().showHistoryScene();
        });

        buttonsBox.getChildren().addAll(onePlayerBtn, twoPlayersBtn, leaderboardBtn, historyBtn);

        // Bảng danh sách người chơi
        Label onlineLabel = new Label("👥 Online Players");
        onlineLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        onlineLabel.setTextFill(Color.web("#2c3e50"));

        onlineTable = new TableView<>();
        onlineTable.setStyle(
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 8;"
        );

        usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setPrefWidth(150);

        statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> {
            String userName = cellData.getValue().getUsername();
            boolean isOnline = Client.getSavedOnlineUsersStatic() != null &&
                    Client.getSavedOnlineUsersStatic().contains(userName);
            return new SimpleStringProperty(isOnline ? "Online" : "Offline");
        });
        statusColumn.setPrefWidth(80);

        scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreColumn.setPrefWidth(80);

        // Hiển thị chấm tròn thay vì chữ Online/Offline
        statusColumn.setCellFactory(column -> new javafx.scene.control.TableCell<UserProfile, String>() {
            private final javafx.scene.shape.Circle statusCircle = new javafx.scene.shape.Circle(6);

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (item.equalsIgnoreCase("Online")) {
                        statusCircle.setFill(javafx.scene.paint.Color.LIMEGREEN);
                    } else {
                        statusCircle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                    }
                    setGraphic(statusCircle);
                    setAlignment(Pos.CENTER);
                    setText(null);
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
                    setAlignment(Pos.CENTER);
                }
            }
        });

        onlineTable.getColumns().addAll(usernameColumn, statusColumn, scoreColumn);
        onlineTable.setPrefHeight(150);
        onlineTable.setMaxWidth(320);
        onlineTable.setPlaceholder(new Text("No players available"));

        // --- Nút Logout ---
        Button logoutBtn = new Button("🚪 LOGOUT");
        logoutBtn.setPrefSize(180, 40);
        logoutBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        logoutBtn.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 20; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        );
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
            "-fx-background-color: #c0392b; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 20; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);"
        ));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 20; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        ));
        logoutBtn.setOnAction(e -> {
            System.out.println("Người dùng đăng xuất: " + client.getUsername());
            client.setUsername(null);
            Main.getInstance().showLoginScene();
        });

        layout.getChildren().addAll(
            icon, title, userInfoBox, spacer1, buttonsBox, 
            onlineLabel, onlineTable, logoutBtn
        );
        
        root.getChildren().add(layout);
        scene = new Scene(root, 600, 750);
    }

    private Button createStyledButton(String text, String color1, String color2) {
        Button btn = new Button(text);
        btn.setPrefSize(280, 45);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        String normalStyle = 
            "-fx-background-color: linear-gradient(to right, " + color1 + ", " + color2 + "); " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 10; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);";
        
        String hoverStyle = 
            "-fx-background-color: linear-gradient(to right, " + color2 + ", " + color1 + "); " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 10; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3); " +
            "-fx-scale-x: 1.05; -fx-scale-y: 1.05;";
        
        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
        
        return btn;
    }

    public Scene getScene() {
        return scene;
    }

    public void updateOnlineList(java.util.List<UserProfile> allUsers) {
        ObservableList<UserProfile> list = FXCollections.observableArrayList(allUsers);
        onlineTable.setItems(list);
    }

}