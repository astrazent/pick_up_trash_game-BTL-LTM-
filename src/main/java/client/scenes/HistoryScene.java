package client.scenes;

import client.Main;
import client.data.MatchHistory;
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

public class HistoryScene {
    private Scene scene;
    private TableView<MatchHistoryEntry> table;

    public HistoryScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Text title = new Text("Match History");
        title.setFont(Font.font(24));

        table = new TableView<>();
        table.setPrefWidth(560);

        TableColumn<MatchHistoryEntry, String> yourNameCol = new TableColumn<>("Your Name");
        yourNameCol.setCellValueFactory(new PropertyValueFactory<>("yourName"));
        yourNameCol.setPrefWidth(120);

        TableColumn<MatchHistoryEntry, String> opponentCol = new TableColumn<>("Opponent");
        opponentCol.setCellValueFactory(new PropertyValueFactory<>("opponentName"));
        opponentCol.setPrefWidth(120);

        TableColumn<MatchHistoryEntry, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setPrefWidth(80);

        TableColumn<MatchHistoryEntry, String> startCol = new TableColumn<>("StartDate");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startCol.setPrefWidth(120);

        TableColumn<MatchHistoryEntry, String> dateCol = new TableColumn<>("EndDate");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePlayed"));
        dateCol.setPrefWidth(120);

        table.getColumns().addAll(yourNameCol, opponentCol, resultCol, startCol, dateCol);

        Button backBtn = new Button("Back to Menu");
        backBtn.setPrefSize(150, 40);
        backBtn.setOnAction(e -> Main.getInstance().showMenuScene());

        layout.getChildren().addAll(title, table, backBtn);
        scene = new Scene(layout, 600, 400);

        // ✅ Gọi load dữ liệu sau khi tạo UI
        //refreshData();
    }

    // client.scenes.HistoryScene.java

// ... (các phần khác của lớp HistoryScene) ...

    public void refreshData() {
        // Gửi yêu cầu lấy lịch sử đấu đến server
        // Dữ liệu sẽ được nhận và cập nhật thông qua Client.handleServerMessage
        Client.getInstance().requestMatchHistory();
        System.out.println("DEBUG (HistoryScene): Yêu cầu lịch sử đấu đã được gửi.");
    }

    // Hàm updateHistory này sẽ được gọi từ Client.java sau khi nhận dữ liệu
    public void updateHistory(List<MatchHistory> matches) {
        System.out.println("updateHistory called. Matches received: " + (matches != null ? matches.size() : "null"));
        ObservableList<MatchHistoryEntry> data = FXCollections.observableArrayList();

        if (matches == null || matches.isEmpty()) { // Xử lý trường hợp không có dữ liệu
            table.setPlaceholder(new Text("No match history found.")); // Hiển thị thông báo khi không có dữ liệu
            table.setItems(data); // Đặt danh sách rỗng
            return;
        }

        String myUsername = Client.getInstance().getUsername();
        for (MatchHistory match : matches) {
            data.add(new MatchHistoryEntry(
                    myUsername,
                    match.getOpponentName(),
                    match.getResult(),
                    match.getStartDate(),
                    match.getGameDate()
            ));
        }

        table.setItems(data);
        table.setPlaceholder(null); // Xóa placeholder nếu có dữ liệu
    }

    public Scene getScene() {
        // Không gọi refreshData() ở đây nữa, vì nó được gọi ở showHistoryScene trong Main.java
        return scene;
    }

// ... (các phần khác của lớp HistoryScene) ...

    // 🧩 Lớp con hiển thị 1 dòng trong bảng
    public static class MatchHistoryEntry {
        private final String yourName;
        private final String opponentName;
        private final String result;
        private final String startDate;
        private final String datePlayed;

        public MatchHistoryEntry(String yourName, String opponentName, String result, String startDate, String datePlayed) {
            this.yourName = yourName;
            this.opponentName = opponentName;
            this.result = result;
            this.startDate = startDate;
            this.datePlayed = datePlayed;
        }

        public String getYourName() { return yourName; }
        public String getOpponentName() { return opponentName; }
        public String getResult() { return result; }
        public String getStartDate() { return startDate; }
        public String getDatePlayed() { return datePlayed; }
    }
}
