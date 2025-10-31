package client.scenes;

import client.Main;
import client.data.MatchHistory;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;

public class LeaderboardScene {
    private Scene scene;
    private TableView<LeaderboardEntry> table;

    public void updateLeaderboard(List<UserProfile> users) {
        System.out.println("updateLeaderboard called. Matches received: " + (users != null ? users.size() : "null"));
        ObservableList<LeaderboardScene.LeaderboardEntry> data = FXCollections.observableArrayList();

        if (users == null || users.isEmpty()) { // Xử lý trường hợp không có dữ liệu
            table.setPlaceholder(new Text("No leaderboard found.")); // Hiển thị thông báo khi không có dữ liệu
            table.setItems(data); // Đặt danh sách rỗng
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
        table.setPlaceholder(null); // Xóa placeholder nếu có dữ liệu
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

        Button backBtn = new Button("Back to Menu");
        backBtn.setPrefSize(150, 40);
        backBtn.setOnAction(e -> {
            Main.getInstance().showMenuScene();
        });

        layout.getChildren().addAll(title, table, backBtn);
        scene = new Scene(layout, 400, 400); // Tăng chiều cao để chứa bảng
    }

    public void refreshData() {
        // Gửi yêu cầu lấy lịch sử đấu đến server
        // Dữ liệu sẽ được nhận và cập nhật thông qua Client.handleServerMessage
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