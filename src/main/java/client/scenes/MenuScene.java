package client.scenes;

import client.Main;
import client.network.Client;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    private static javafx.stage.Popup activePopup = null;
    private static String activePopupUsername = null;
    private static Button logoutBtnRef;

    public MenuScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Text title = new Text("Main Menu");
        title.setFont(Font.font(24));

        // --- HIỂN THỊ THÔNG TIN NGƯỜI DÙNG ---
        Client client = Client.getInstance();
        String username = client.getUsername();
        int highScore = client.getHighScore();

        Text userInfo = new Text("👤 " + username + " — High Score: " + highScore);
        userInfo.setFont(Font.font("Segoe UI", 18));
        userInfo.setFill(Color.web("#2a9d8f")); // Xanh ngọc dịu, nổi bật hơn
        userInfo.setStyle("-fx-font-weight: bold;");



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

        // --- Nút Logout ---
        Button logoutBtn = new Button("Logout");
        logoutBtn.setPrefSize(150, 40);
        logoutBtn.setStyle("-fx-background-color: #e63946; -fx-text-fill: white; -fx-font-weight: bold;");
        logoutBtn.setOnAction(e -> {
            // Xóa thông tin người dùng, trở lại màn hình login
            System.out.println("Người dùng đăng xuất: " + client.getUsername());
            client.sendMessage("LOGOUT;"+client.getUsername());
            Main.getInstance().showLoginScene();
        });

        // Bảng danh sách người chơi
        onlineTable = new TableView<>();

        usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setPrefWidth(120);

        statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> {
            String userName = cellData.getValue().getUsername();
            boolean isOnline = Client.getSavedOnlineUsersStatic() != null &&
                    Client.getSavedOnlineUsersStatic().contains(userName);
            return new SimpleStringProperty(isOnline ? "Online" : "Offline");
        });
        statusColumn.setPrefWidth(60);

        scoreColumn = new TableColumn<>("High Score");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreColumn.setPrefWidth(80);

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

        double totalWidth = 0;
        for (TableColumn<?, ?> col : onlineTable.getColumns()) {
            totalWidth += col.getPrefWidth();
        }

        onlineTable.setMaxWidth(totalWidth + 2);
        onlineTable.setPrefHeight(150);
        onlineTable.setPlaceholder(new Text("No players available"));

        layout.getChildren().addAll(title, userInfo, logoutBtn, onePlayerBtn, twoPlayersBtn, leaderboardBtn, historyBtn, onlineTable);
        scene = new Scene(layout, 400, 600);

        logoutBtnRef = logoutBtn;
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