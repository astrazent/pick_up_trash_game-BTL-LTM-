package client.scenes;

import client.Main;
import client.data.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;

public class LeaderboardScene {
    private Scene scene;
    private TableView<LeaderboardEntry> table;
    public void updateLeaderboard(List<UserProfile> users) {
        for (UserProfile user : users) {
            System.out.println("UserId: " + user.getUserId()
                    + ", Username: " + user.getUsername());
        }
        ObservableList<LeaderboardEntry> data = FXCollections.observableArrayList();
        if (users == null) {
            table.setItems(data); // Xóa bảng nếu không có dữ liệu
            return;
        }

        int rank = 1;
        for (UserProfile user : users) {
            data.add(new LeaderboardEntry(rank++, user.getUsername(), user.getScore()));
        }

        table.setItems(data);
    }

    public LeaderboardScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Text title = new Text("Leaderboard");
        title.setFont(Font.font(24));

        // Tạo bảng
        table = new TableView<>();
        table.setPrefWidth(350);

        // Tạo các cột
        TableColumn<LeaderboardEntry, Integer> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setPrefWidth(75);

        TableColumn<LeaderboardEntry, String> nameCol = new TableColumn<>("Player");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(175);

        TableColumn<LeaderboardEntry, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setPrefWidth(100);

        table.getColumns().addAll(rankCol, nameCol, scoreCol);

        // TODO: Lấy dữ liệu thực từ server và cập nhật bảng
        // Dữ liệu mẫu để hiển thị
        ObservableList<LeaderboardEntry> data = FXCollections.observableArrayList(
                new LeaderboardEntry(1, "PlayerOne", 1500),
                new LeaderboardEntry(2, "PlayerTwo", 1200),
                new LeaderboardEntry(3, "PlayerThree", 950)
        );
        table.setItems(data);

        Button backBtn = new Button("Back to Menu");
        backBtn.setPrefSize(150, 40);
        backBtn.setOnAction(e -> {
            Main.getInstance().showMenuScene();
        });

        layout.getChildren().addAll(title, table, backBtn);
        scene = new Scene(layout, 400, 400); // Tăng chiều cao để chứa bảng
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