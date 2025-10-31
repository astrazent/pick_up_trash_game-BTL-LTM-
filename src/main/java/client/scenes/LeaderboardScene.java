package client.scenes;

import java.util.List;

import client.Main;
import client.data.UserProfile;
import client.network.Client;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class LeaderboardScene {
    private Scene scene;
    private TableView<LeaderboardEntry> table;

    public void updateLeaderboard(List<UserProfile> users) {
        System.out.println("updateLeaderboard called. Matches received: " + (users != null ? users.size() : "null"));
        ObservableList<LeaderboardScene.LeaderboardEntry> data = FXCollections.observableArrayList();

        if (users == null || users.isEmpty()) {
            table.setPlaceholder(new Text("No leaderboard found."));
            table.setItems(data);
            return;
        }

        int rank = 1;
        for (UserProfile userProfile : users) {
            data.add(new LeaderboardEntry(
                    rank++,
                    userProfile.getUsername(),
                    userProfile.getScore()
            ));
        }

        table.setItems(data);
        table.setPlaceholder(null);
    }

    public LeaderboardScene() {
        // Container chính với gradient background
        StackPane root = new StackPane();
        
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#f093fb")),
            new Stop(1, Color.web("#f5576c"))
        );
        BackgroundFill bgFill = new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY);
        root.setBackground(new Background(bgFill));

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setMaxWidth(550);
        layout.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95); " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );

        // Icon và tiêu đề
        Text icon = new Text("🏆");
        icon.setFont(Font.font(50));

        Text title = new Text("LEADERBOARD");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setFill(Color.web("#2c3e50"));

        Text subtitle = new Text("Top Players Rankings");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subtitle.setFill(Color.web("#7f8c8d"));

        // Tạo bảng với styling đẹp
        table = new TableView<>();
        table.setPrefWidth(480);
        table.setPrefHeight(350);
        table.setStyle(
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 10;"
        );

        // Tạo các cột
        TableColumn<LeaderboardEntry, Integer> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setPrefWidth(80);
        rankCol.setStyle("-fx-alignment: CENTER;");
        
        // Custom cell factory để thêm icon cho rank
        rankCol.setCellFactory(column -> new javafx.scene.control.TableCell<LeaderboardEntry, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String displayText = "";
                    String style = "-fx-alignment: CENTER; -fx-font-weight: bold; ";
                    
                    if (item == 1) {
                        displayText = "🥇 " + item;
                        style += "-fx-text-fill: #f39c12;";
                    } else if (item == 2) {
                        displayText = "🥈 " + item;
                        style += "-fx-text-fill: #95a5a6;";
                    } else if (item == 3) {
                        displayText = "🥉 " + item;
                        style += "-fx-text-fill: #cd7f32;";
                    } else {
                        displayText = String.valueOf(item);
                        style += "-fx-text-fill: #34495e;";
                    }
                    
                    setText(displayText);
                    setStyle(style);
                }
            }
        });

        TableColumn<LeaderboardEntry, String> nameCol = new TableColumn<>("Player");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        nameCol.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<LeaderboardEntry, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setPrefWidth(150);
        scoreCol.setStyle("-fx-alignment: CENTER;");
        
        // Custom cell factory để làm nổi bật điểm cao
        scoreCol.setCellFactory(column -> new javafx.scene.control.TableCell<LeaderboardEntry, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("⭐ " + item);
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                }
            }
        });

        table.getColumns().addAll(rankCol, nameCol, scoreCol);

        Button backBtn = new Button("⬅ BACK TO MENU");
        backBtn.setPrefSize(200, 45);
        backBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        backBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 25; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        );
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #2980b9, #3498db); " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 25; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);"
        ));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 25; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        ));
        backBtn.setOnAction(e -> Main.getInstance().showMenuScene());

        layout.getChildren().addAll(icon, title, subtitle, table, backBtn);
        
        root.getChildren().add(layout);
        scene = new Scene(root, 650, 650);
    }

    public void refreshData() {
        Client.getInstance().requestLeaderboard();
        System.out.println("DEBUG (LeaderboardScene): Yêu cầu lịch sử đấu đã được gửi.");
    }

    public Scene getScene() {
        return scene;
    }

    // Lớp nội tại để biểu diễn một mục trong bảng xếp hạng
    public static class LeaderboardEntry {
        private final int rank;
        private final String name;
        private final int score;

        public LeaderboardEntry(int rank, String name, int score) {
            this.rank = rank;
            this.name = name;
            this.score = score;
        }

        public int getRank() {
            return rank;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }
    }
}