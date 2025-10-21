package gamePlay.scenes;

import gamePlay.Main;
import gamePlay.data.MatchHistory;
import gamePlay.network.Client;
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

public class HistoryScene {
    private Scene scene;
    private TableView<MatchHistoryEntry> table;

    // Phương thức để cập nhật bảng với dữ liệu thật từ server
    public void updateHistory(List<MatchHistory> matches) {
        ObservableList<MatchHistoryEntry> data = FXCollections.observableArrayList();
        if (matches == null) {
            table.setItems(data); // Xóa bảng nếu không có dữ liệu
            return;
        }

        // Lấy tên người dùng hiện tại để hiển thị vào cột "Your Name"
        String myUsername = Client.getInstance().getUsername();

        for (MatchHistory match : matches) {
            data.add(new MatchHistoryEntry(
                    myUsername,
                    match.getOpponentName(),
                    match.getResult(),
                    match.getGameDate()
            ));
        }

        table.setItems(data);
    }

    public HistoryScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Text title = new Text("Match History");
        title.setFont(Font.font(24));

        // Tạo bảng
        table = new TableView<>();
        table.setPrefWidth(450); // Tăng chiều rộng để vừa 4 cột

        // Tạo các cột
        TableColumn<MatchHistoryEntry, String> yourNameCol = new TableColumn<>("Your Name");
        yourNameCol.setCellValueFactory(new PropertyValueFactory<>("yourName"));
        yourNameCol.setPrefWidth(120);

        TableColumn<MatchHistoryEntry, String> opponentCol = new TableColumn<>("Opponent");
        opponentCol.setCellValueFactory(new PropertyValueFactory<>("opponentName"));
        opponentCol.setPrefWidth(120);

        TableColumn<MatchHistoryEntry, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setPrefWidth(80);

        TableColumn<MatchHistoryEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePlayed"));
        dateCol.setPrefWidth(120);


        table.getColumns().addAll(yourNameCol, opponentCol, resultCol, dateCol);

        // Dữ liệu mẫu để hiển thị lúc đầu
        ObservableList<MatchHistoryEntry> sampleData = FXCollections.observableArrayList(
                new MatchHistoryEntry("MyPlayer", "OpponentA", "WIN", "2025-10-20"),
                new MatchHistoryEntry("MyPlayer", "OpponentB", "LOSS", "2025-10-19")
        );
        table.setItems(sampleData);


        Button backBtn = new Button("Back to Menu");
        backBtn.setPrefSize(150, 40);
        backBtn.setOnAction(e -> {
            Main.getInstance().showMenuScene();
        });

        layout.getChildren().addAll(title, table, backBtn);
        scene = new Scene(layout, 500, 400); // Tăng kích thước cửa sổ
    }

    public Scene getScene() {
        return scene;
    }

    // Lớp nội tại để biểu diễn một hàng trong bảng lịch sử đấu
    public static class MatchHistoryEntry {
        private final String yourName;
        private final String opponentName;
        private final String result;
        private final String datePlayed;

        public MatchHistoryEntry(String yourName, String opponentName, String result, String datePlayed) {
            this.yourName = yourName;
            this.opponentName = opponentName;
            this.result = result;
            this.datePlayed = datePlayed;
        }

        // Getters phải có để PropertyValueFactory hoạt động
        public String getYourName() { return yourName; }
        public String getOpponentName() { return opponentName; }
        public String getResult() { return result; }
        public String getDatePlayed() { return datePlayed; }
    }
}