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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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


        TableColumn<MatchHistoryEntry, String> opponentCol = new TableColumn<>("Opponent");
        opponentCol.setCellValueFactory(new PropertyValueFactory<>("opponentName"));
        opponentCol.setPrefWidth(120);

        TableColumn<MatchHistoryEntry, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setPrefWidth(60);
        resultCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MatchHistoryEntry, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startCol.setPrefWidth(120);
        startCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MatchHistoryEntry, String> dateCol = new TableColumn<>("End Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePlayed"));
        dateCol.setPrefWidth(120);
        dateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MatchHistoryEntry, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationCol.setPrefWidth(80);
        durationCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(opponentCol, resultCol, startCol, dateCol, durationCol);

        double totalWidth = 0;
        for (TableColumn<?, ?> col : table.getColumns()) {
            totalWidth += col.getPrefWidth();
        }

        table.setMaxWidth(totalWidth + 16);

        Button backBtn = new Button("Back to Menu");
        backBtn.setPrefSize(150, 40);
        backBtn.setOnAction(e -> Main.getInstance().showMenuScene());

        layout.getChildren().addAll(title, table, backBtn);
        scene = new Scene(layout, 600, 400);
    }

    public void refreshData() {
        Client.getInstance().requestMatchHistory();
        System.out.println("DEBUG (HistoryScene): Yêu cầu lịch sử đấu đã được gửi.");
    }

    public void updateHistory(List<MatchHistory> matches) {
        System.out.println("updateHistory called. Matches received: " + (matches != null ? matches.size() : "null"));
        ObservableList<MatchHistoryEntry> data = FXCollections.observableArrayList();

        if (matches == null || matches.isEmpty()) {
            table.setPlaceholder(new Text("No match history found."));
            table.setItems(data);
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
        table.setPlaceholder(null);
    }

    public Scene getScene() {
        return scene;
    }

    // 🧩 Lớp con hiển thị 1 dòng trong bảng
    public static class MatchHistoryEntry {
        private final String yourName;
        private final String opponentName;
        private final String result;
        private final String startDate;
        private final String datePlayed;
        private final String duration; // ⏱thêm thuộc tính mới

        private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public MatchHistoryEntry(String yourName, String opponentName, String result, String startDate, String datePlayed) {
            this.yourName = yourName;
            this.opponentName = opponentName;
            this.result = result;
            this.startDate = formatDate(startDate);
            this.datePlayed = formatDate(datePlayed);

            // Tính thời lượng trận đấu nếu có đủ thông tin
            String computedDuration = "N/A";
            try {
                if (startDate != null && datePlayed != null &&
                        !startDate.isEmpty() && !datePlayed.isEmpty()) {

                    LocalDateTime start = LocalDateTime.parse(startDate, FORMATTER);
                    LocalDateTime end = LocalDateTime.parse(datePlayed, FORMATTER);
                    Duration d = Duration.between(start, end);

                    long minutes = d.toMinutes();
                    long seconds = d.getSeconds() % 60;
                    computedDuration = String.format("%02d:%02d", minutes, seconds);
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi tính duration: " + e.getMessage());
            }

            this.duration = computedDuration;
        }

        // Hàm tiện ích để đổi format
        private static String formatDate(String dateStr) {
            try {
                if (dateStr == null || dateStr.isEmpty()) return "N/A";
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, INPUT_FORMATTER);
                // Gán múi giờ Việt Nam
                ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
                ZonedDateTime vnTime = dateTime.atZone(vietnamZone);
                return vnTime.format(OUTPUT_FORMATTER);
            } catch (Exception e) {
                return dateStr; // fallback giữ nguyên nếu parse lỗi
            }
        }

        public String getYourName() { return yourName; }
        public String getOpponentName() { return opponentName; }
        public String getResult() { return result; }
        public String getStartDate() { return startDate; }
        public String getDatePlayed() { return datePlayed; }
        public String getDuration() { return duration; }
    }
}
