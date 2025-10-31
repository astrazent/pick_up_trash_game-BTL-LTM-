package client.scenes;

import java.util.List;

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

public class HistoryScene {
    private Scene scene;
    private TableView<MatchHistoryEntry> table;

    public HistoryScene() {
        // Container chính với gradient background
        StackPane root = new StackPane();
        
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#4facfe")),
            new Stop(1, Color.web("#00f2fe"))
        );
        BackgroundFill bgFill = new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY);
        root.setBackground(new Background(bgFill));

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setMaxWidth(700);
        layout.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95); " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );

        // Icon và tiêu đề
        Text icon = new Text("📜");
        icon.setFont(Font.font(50));

        Text title = new Text("MATCH HISTORY");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setFill(Color.web("#2c3e50"));

        Text subtitle = new Text("Your Recent Battles");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subtitle.setFill(Color.web("#7f8c8d"));

        table = new TableView<>();
        table.setPrefWidth(640);
        table.setPrefHeight(350);
        table.setStyle(
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 10;"
        );

        TableColumn<MatchHistoryEntry, String> yourNameCol = new TableColumn<>("Your Name");
        yourNameCol.setCellValueFactory(new PropertyValueFactory<>("yourName"));
        yourNameCol.setPrefWidth(120);
        yourNameCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MatchHistoryEntry, String> opponentCol = new TableColumn<>("Opponent");
        opponentCol.setCellValueFactory(new PropertyValueFactory<>("opponentName"));
        opponentCol.setPrefWidth(120);
        opponentCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MatchHistoryEntry, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setPrefWidth(100);
        resultCol.setStyle("-fx-alignment: CENTER;");
        
        // Custom cell factory để tô màu kết quả
        resultCol.setCellFactory(column -> new javafx.scene.control.TableCell<MatchHistoryEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String displayText = "";
                    String style = "-fx-alignment: CENTER; -fx-font-weight: bold; ";
                    
                    if (item.equalsIgnoreCase("WIN")) {
                        displayText = "✅ WIN";
                        style += "-fx-text-fill: #27ae60; -fx-background-color: #d5f4e6;";
                    } else if (item.equalsIgnoreCase("LOSE")) {
                        displayText = "❌ LOSE";
                        style += "-fx-text-fill: #e74c3c; -fx-background-color: #fadbd8;";
                    } else if (item.equalsIgnoreCase("DRAW")) {
                        displayText = "➖ DRAW";
                        style += "-fx-text-fill: #f39c12; -fx-background-color: #fef5e7;";
                    } else {
                        displayText = item;
                        style += "-fx-text-fill: #34495e;";
                    }
                    
                    setText(displayText);
                    setStyle(style);
                }
            }
        });

        TableColumn<MatchHistoryEntry, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startCol.setPrefWidth(150);
        startCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MatchHistoryEntry, String> dateCol = new TableColumn<>("End Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePlayed"));
        dateCol.setPrefWidth(150);
        dateCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(yourNameCol, opponentCol, resultCol, startCol, dateCol);

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
        scene = new Scene(root, 750, 650);
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
