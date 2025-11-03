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

    private static javafx.stage.Popup activePopup = null;
    private static String activePopupUsername = null;
    private static Button logoutBtnRef;

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

        double totalWidth = 0;
        for (TableColumn<?, ?> col : onlineTable.getColumns()) {
            totalWidth += col.getPrefWidth();
        }

        onlineTable.setMaxWidth(totalWidth + 2);
        onlineTable.setPrefHeight(150);
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
            client.sendMessage("LOGOUT;"+client.getUsername());
            Main.getInstance().showLoginScene();
        });

        logoutBtnRef = logoutBtn;

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

        // --- Khi click vào một hàng ---
        onlineTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<UserProfile> row = new javafx.scene.control.TableRow<>();

            row.setOnMouseClicked(event -> {
                UserProfile clickedUser = row.getItem();
                if (clickedUser == null) return;

                String clickedUsername = clickedUser.getUsername();
                String currentUser = Client.getInstance().getUsername();
                boolean isOnline = Client.getSavedOnlineUsersStatic().contains(clickedUsername);

                // Không cho click chính mình
                if (clickedUsername.equals(currentUser)) {
                    return;
                }

                // Chỉ hiển thị popup nếu người chơi đang online
                if (!isOnline) return;

                // Nếu popup cùng người này đang mở → đóng lại thay vì mở thêm
                if (activePopup != null && clickedUsername.equals(activePopupUsername)) {
                    activePopup.hide();
                    activePopup = null;
                    activePopupUsername = null;
                    return;
                }

                // Đóng popup cũ (nếu có)
                if (activePopup != null) {
                    activePopup.hide();
                }

                // Tạo popup mới
                javafx.stage.Popup popup = new javafx.stage.Popup();
                javafx.scene.layout.VBox popupContent = new javafx.scene.layout.VBox(8);
                popupContent.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
                popupContent.setAlignment(Pos.CENTER);

                javafx.scene.text.Text text = new javafx.scene.text.Text("Gửi lời mời thách đấu?");
                text.setFont(Font.font("Segoe UI", 14));

                javafx.scene.control.Button acceptBtn = new javafx.scene.control.Button("✔");
                acceptBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 50;");
                javafx.scene.control.Button declineBtn = new javafx.scene.control.Button("✖");
                declineBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 50;");

                javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10, acceptBtn, declineBtn);
                buttonBox.setAlignment(Pos.CENTER);

                popupContent.getChildren().addAll(text, buttonBox);
                popup.getContent().add(popupContent);

                // Hiển thị popup ngay trên dòng được click, cao hơn một chút
                double popupX = event.getScreenX() - popupContent.getWidth() / 2;
                double popupY = event.getScreenY() - 90; // nâng lên một chút
                popup.show(scene.getWindow(), popupX, popupY);

                // Lưu lại popup hiện tại
                activePopup = popup;
                activePopupUsername = clickedUsername;

                // Xử lý nút trong popup
                acceptBtn.setOnAction(e -> {
                    // Đánh dấu người chơi đang chờ phản hồi
                    Client.getInstance().sendMessage("WAITING_ON");
                    Client.getInstance().sendMessage("SET_OPPONENT_NAME;"+clickedUsername);
                    Main.getInstance().showWaitingAcceptanceScene(clickedUsername);
                    popup.hide();
                    activePopup = null;
                    activePopupUsername = null;
                });

                declineBtn.setOnAction(e -> {
                    popup.hide();
                    activePopup = null;
                    activePopupUsername = null;
                });
            });

            return row;
        });

    }

    // HIỂN THỊ POPUP KHI NHẬN LỜI THÁCH ĐẤU
    public static void showChallengePopup(String senderUsername) {
        javafx.application.Platform.runLater(() -> {
            if (activePopup != null) {
                activePopup.hide();
            }

            if (logoutBtnRef != null) {
                logoutBtnRef.setDisable(true); // Vô hiệu hóa logout khi popup hiện
            }

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

            javafx.scene.text.Text text = new javafx.scene.text.Text(
                    "Bạn nhận được lời thách đấu từ " + senderUsername
            );
            text.setFont(Font.font("Segoe UI", 16));
            text.setFill(Color.web("#2a9d8f"));

            javafx.scene.control.Button acceptBtn = new javafx.scene.control.Button("✔");
            acceptBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 50;");

            javafx.scene.control.Button declineBtn = new javafx.scene.control.Button("✖");
            declineBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 50;");

            javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(15, acceptBtn, declineBtn);
            buttonBox.setAlignment(Pos.CENTER);

            popupContent.getChildren().addAll(text, buttonBox);
            popup.getContent().add(popupContent);

            javafx.stage.Window window = Main.getPrimaryStage();
            double centerX = window.getX() + window.getWidth() / 2 - 150;
            double centerY = window.getY() + window.getHeight() / 2 - 75;
            popup.show(window, centerX, centerY);

            activePopup = popup;
            activePopupUsername = senderUsername;

            // Khi Accept hoặc Decline -> đóng popup + bật lại logout
            acceptBtn.setOnAction(e -> {
                Client.getInstance().sendMessage("ACCEPT_CHALLENGE;" + senderUsername);
                popup.hide();
                activePopup = null;
                activePopupUsername = null;
                if (logoutBtnRef != null) logoutBtnRef.setDisable(false); // Bật lại
            });

            declineBtn.setOnAction(e -> {
                Client.getInstance().sendMessage("DECLINE_CHALLENGE;" + senderUsername);
                popup.hide();
                activePopup = null;
                activePopupUsername = null;
                if (logoutBtnRef != null) logoutBtnRef.setDisable(false); // Bật lại
            });
        });
    }


    public static void showChallengeDeclinedPopup() {
        javafx.application.Platform.runLater(() -> {
            // Nếu popup cũ còn mở thì đóng lại
            if (activePopup != null) {
                activePopup.hide();
            }

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

            javafx.scene.text.Text text = new javafx.scene.text.Text(
                    "Đối thủ đã từ chối lời thách đấu"
            );
            text.setFont(Font.font("Segoe UI", 16));
            text.setFill(Color.web("#e74c3c")); // tông đỏ nhẹ để thể hiện từ chối

            // Nút OK để đóng popup
            javafx.scene.control.Button okButton = new javafx.scene.control.Button("OK");
            okButton.setStyle(
                    "-fx-background-color: #2ecc71; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-pref-width: 80;"
            );

            popupContent.getChildren().addAll(text, okButton);
            popup.getContent().add(popupContent);

            // Lấy cửa sổ hiện tại để định vị popup giữa màn hình
            javafx.stage.Window window = Main.getPrimaryStage();
            double centerX = window.getX() + window.getWidth() / 2 - 150;
            double centerY = window.getY() + window.getHeight() / 2 - 75;
            popup.show(window, centerX, centerY);

            // Lưu lại popup hiện tại
            activePopup = popup;

            // Xử lý nút OK
            okButton.setOnAction(e -> {
                popup.hide();
                activePopup = null;
                activePopupUsername = null;
            });
        });
    }

    public static void showOpponentOfflinePopup() {
        javafx.application.Platform.runLater(() -> {
            // Nếu popup cũ còn mở thì đóng lại
            if (activePopup != null) {
                activePopup.hide();
            }

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

            javafx.scene.text.Text text = new javafx.scene.text.Text(
                    "Đối thủ đã ngắt kết nối"
            );
            text.setFont(Font.font("Segoe UI", 16));
            text.setFill(Color.web("#e74c3c")); // tông đỏ nhẹ để thể hiện từ chối

            // Nút OK để đóng popup
            javafx.scene.control.Button okButton = new javafx.scene.control.Button("OK");
            okButton.setStyle(
                    "-fx-background-color: #2ecc71; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-pref-width: 80;"
            );

            popupContent.getChildren().addAll(text, okButton);
            popup.getContent().add(popupContent);

            // Lấy cửa sổ hiện tại để định vị popup giữa màn hình
            javafx.stage.Window window = Main.getPrimaryStage();
            double centerX = window.getX() + window.getWidth() / 2 - 150;
            double centerY = window.getY() + window.getHeight() / 2 - 75;
            popup.show(window, centerX, centerY);

            // Lưu lại popup hiện tại
            activePopup = popup;

            // Xử lý nút OK
            okButton.setOnAction(e -> {
                popup.hide();
                if (logoutBtnRef != null) logoutBtnRef.setDisable(false);
                activePopup = null;
                activePopupUsername = null;
            });
        });
    }
}