package client.scenes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import client.Main;
import client.config.GameConfig;
import client.game.GameLoop;
import client.game.Player;
import client.game.Trash;
import client.game.TrashBin;
import client.game.TrashType;
import client.input.InputHandler;
import client.network.Client;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public class GameScene {
    private final Scene scene;
    private final Pane root;
    private GameLoop gameLoop;

    private Player player1;
    private Player player2;
    private final List<Trash> trashList = new ArrayList<>();
    private final List<TrashBin> trashBins = new ArrayList<>();

    // --- UI Elements ---
    private final Label scoreLabel1;
    private Label scoreLabel2; // <-- THAY ĐỔI: Có thể không được khởi tạo
    private final Label timerLabel;
    private Button settingsButton;
    private VBox pauseMenu;
    private Button pauseResumeButton;
    private Button surrenderButton; // Nút thoát (đầu hàng)
    private Label pauseStatusLabel;
    private Label pauseChancesLabel;

    // --- THÊM: Các yếu tố UI cho chế độ 1 người chơi ---
    private HBox heartsBox; // Container cho các trái tim
    private final List<ImageView> heartImageViews = new ArrayList<>();
    private int playerLives = 3; // Số mạng ban đầu

    // --- THÊM: Các yếu tố UI cho Chat ---
    private VBox chatBox;
    private VBox chatMessagesContainer;
    private ScrollPane chatScrollPane;
    private TextField chatInput;
    private Button chatSendButton;
    private Button chatToggleButton;
    private boolean isChatVisible = false;
    private int unreadMessageCount = 0; // Số tin nhắn chưa đọc
    
    // Biến cho kéo thả nút chat
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    public GameScene(int playerCount, String p1Name, String p2Name) {
        System.out.println("check_game-scene: "+p1Name+" "+p2Name);
        root = new Pane();
        GameConfig config = Main.getInstance().getGameConfig();
        scene = new Scene(root, config.window.width, config.window.height);

        // --- UI Elements ---
        scoreLabel1 = new Label(p1Name + ": 0");
        scoreLabel1.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        scoreLabel1.setTextFill(Color.RED);
        scoreLabel1.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 1, 1, 1);");
        scoreLabel1.setTranslateX(30);
        scoreLabel1.setTranslateY(20);

        timerLabel = new Label("02:00");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 3, 1, 2, 2);");
        timerLabel.layoutXProperty().bind(root.widthProperty().subtract(timerLabel.widthProperty()).divide(2));
        timerLabel.setTranslateY(20);

        // Thêm background image
        setupBackground();

        // --- Game Objects ---
        setupPlayers(playerCount, config, p1Name, p2Name);
        setupTrashBins(config);
        setupSettingsAndPauseMenu();
        setupChatUI(config); // Thiết lập UI chat

        root.getChildren().addAll(scoreLabel1, timerLabel, settingsButton, pauseMenu);
        
        // Làm cho scene responsive khi thay đổi kích thước cửa sổ
        setupResponsiveLayout(playerCount);

        // --- Input and GameLoop ---
        InputHandler inputHandler = new InputHandler(scene);
        gameLoop = new GameLoop(this, inputHandler, player1, player2, trashList, trashBins);
    }
    
    // Thiết lập responsive layout
    private void setupResponsiveLayout(int playerCount) {
        // Listener để điều chỉnh giới hạn khi resize
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            // Giới hạn player trong màn hình mới
            double newWidth = newVal.doubleValue();
            if (player1 != null && player1.getX() + player1.getWidth() > newWidth) {
                player1.setX(newWidth - player1.getWidth());
            }
            if (player2 != null && player2.getX() + player2.getWidth() > newWidth) {
                player2.setX(newWidth - player2.getWidth());
            }
        });
        
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            // Giới hạn player trong màn hình mới
            double newHeight = newVal.doubleValue();
            if (player1 != null && player1.getY() + player1.getHeight() > newHeight) {
                player1.setY(newHeight - player1.getHeight());
            }
            if (player2 != null && player2.getY() + player2.getHeight() > newHeight) {
                player2.setY(newHeight - player2.getHeight());
            }
        });
    }
    
    private void setupBackground() {
        try {
            File bgFile = new File("assets/images/backgrounds/bg.png");
            if (bgFile.exists()) {
                Image backgroundImage = new Image(bgFile.toURI().toString());
                ImageView backgroundView = new ImageView(backgroundImage);
                
                // Bind kích thước background với scene để tự động scale
                backgroundView.fitWidthProperty().bind(scene.widthProperty());
                backgroundView.fitHeightProperty().bind(scene.heightProperty());
                backgroundView.setPreserveRatio(false);
                
                // Đặt background ở dưới cùng
                root.getChildren().add(0, backgroundView);
            } else {
                // Fallback về màu nền nếu không tìm thấy ảnh
                root.setStyle("-fx-background-color: #87CEEB;"); // Sky blue
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi load background: " + e.getMessage());
            root.setStyle("-fx-background-color: #87CEEB;");
        }
    }

    private void setupPlayers(int playerCount, GameConfig config, String p1Name, String p2Name) {
        player1 = new Player(config.window.width / 2.0 - 80, config.window.height - config.player.height - 20, p1Name, 1);
        root.getChildren().add(player1.getView());

        if (playerCount == 2) {
            // Player 2
            player2 = new Player(config.window.width / 2.0 + 80, config.window.height - config.player.height - 20, p2Name, 2);
            root.getChildren().add(player2.getView());

            // Score label 2 - Bind vị trí với kích thước scene
            scoreLabel2 = new Label(p2Name + ": 0");
            scoreLabel2.setFont(Font.font("Arial", FontWeight.BOLD, 22));
            scoreLabel2.setTextFill(Color.RED);
            scoreLabel2.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 1, 1, 1);");
            scoreLabel2.translateXProperty().bind(scene.widthProperty().subtract(200));
            scoreLabel2.setTranslateY(20);
            root.getChildren().add(scoreLabel2);
        } else {
            // --- MỚI: Thiết lập hiển thị mạng cho chế độ 1 người chơi ---
            setupLivesDisplay(config);
        }
    }

    // --- MỚI: Phương thức thiết lập hiển thị mạng (trái tim) ---
    private void setupLivesDisplay(GameConfig config) {
        heartsBox = new HBox(10); // 10 là khoảng cách giữa các trái tim
        heartsBox.setAlignment(Pos.CENTER);
        // Bind vị trí với kích thước scene
        heartsBox.layoutXProperty().bind(scene.widthProperty().subtract(250));
        heartsBox.setLayoutY(20);

        try {
            // Thay "resources/heart.png" bằng đường dẫn chính xác đến file ảnh của bạn
            Image heartImage = new Image(new FileInputStream("src/main/resources/images/heart.png"));
            for (int i = 0; i < 3; i++) { // Luôn tạo 3 trái tim
                ImageView heartView = new ImageView(heartImage);
                heartView.setFitHeight(40);
                heartView.setFitWidth(40);
                heartView.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 1, 1, 1);");
                heartImageViews.add(heartView);
                heartsBox.getChildren().add(heartView);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Không tìm thấy file ảnh trái tim! 'resources/heart.png'");
            // Thay thế bằng text nếu không có ảnh
            Label livesLabel = new Label("Mạng: " + playerLives);
            livesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
            livesLabel.setTextFill(Color.RED);
            livesLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 1, 1, 1);");
            heartsBox.getChildren().add(livesLabel);
        }

        root.getChildren().add(heartsBox);
    }

    // --- MỚI: Phương thức để xử lý khi người chơi mất một mạng ---
    public void playerLosesLife(String playerName) {
        if (!playerName.equals(player1.getUsername())) {
            System.out.println("Lỗi: tên người chơi không hợp lệ (" + playerName + ")");
            return;
        }

        // Chỉ xử lý mất mạng trong chế độ 1 người
        if (player2 != null) {
            // Chế độ 2 người chơi: không trừ mạng, chỉ thua khi hết giờ hoặc đầu hàng
            return;
        }

        if (!playerName.equals(player1.getUsername())) {
            System.out.println("Lỗi: tên người chơi không hợp lệ (" + playerName + ")");
            return;
        }

        if (playerLives > 0) {
            playerLives--;
            updateLivesDisplay();

            if (playerLives <= 0) {
                showGameOver(player1.getUsername());
            }
        }

        // Trong mode 2 player, không xử lý gì cả (chỉ trừ điểm đã được xử lý ở server)
    }

    // --- MỚI: Cập nhật giao diện hiển thị số mạng còn lại ---
    private void updateLivesDisplay() {
        // Ẩn trái tim dựa trên số mạng còn lại
        for (int i = 0; i < heartImageViews.size(); i++) {
            if (i < playerLives) {
                heartImageViews.get(i).setVisible(true);
            } else {
                heartImageViews.get(i).setVisible(false);
            }
        }
    }


    private void setupTrashBins(GameConfig config) {
        TrashType[] types = TrashType.values();
        int binCount = types.length;

        // Tính toán ban đầu
        double binWidth = (double) config.window.width / binCount;
        double binHeight = 90;
        double yPos = config.window.height - binHeight;

        for (int i = 0; i < binCount; i++) {
            double xPos = i * binWidth;
            TrashBin bin = new TrashBin(xPos, yPos, binWidth, binHeight - 30, types[i]);
            trashBins.add(bin);
            root.getChildren().add(bin.getView());
            
            // Thêm label tên loại rác với style đẹp hơn
            Label binLabel = new Label(getBinTypeName(types[i]));
            binLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            binLabel.setTextFill(Color.WHITE);
            binLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
            
            // Bind vị trí label với kích thước scene để responsive
            final int binIndex = i;
            binLabel.layoutXProperty().bind(
                scene.widthProperty()
                    .divide(binCount)
                    .multiply(binIndex)
                    .add(scene.widthProperty().divide(binCount).divide(2))
                    .subtract(40)
            );
            binLabel.layoutYProperty().bind(scene.heightProperty().subtract(25));
            
            root.getChildren().add(binLabel);
        }
        
        // Listener để cập nhật vị trí và kích thước bins khi resize
        scene.widthProperty().addListener((obs, oldVal, newVal) -> updateTrashBinsLayout());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> updateTrashBinsLayout());
    }
    
    // Cập nhật layout của trash bins khi resize
    private void updateTrashBinsLayout() {
        if (trashBins.isEmpty()) return;
        
        double sceneWidth = scene.getWidth();
        double sceneHeight = scene.getHeight();
        int binCount = trashBins.size();
        double binWidth = sceneWidth / binCount;
        double binHeight = 90;
        double yPos = sceneHeight - binHeight;
        
        for (int i = 0; i < trashBins.size(); i++) {
            TrashBin bin = trashBins.get(i);
            double xPos = i * binWidth;
            bin.updatePosition(xPos, yPos, binWidth, binHeight - 30);
        }
    }
    
    private String getBinTypeName(TrashType type) {
        switch (type) {
            case ORGANIC:
                return "Hữu cơ";
            case PLASTIC:
                return "Nhựa";
            case METAL:
                return "Kim loại";
            case PAPER:
                return "Giấy";
            default:
                return type.name();
        }
    }

    private void setupSettingsAndPauseMenu() {
        settingsButton = new Button("⚙ Tùy chọn");
        settingsButton.setFocusTraversable(false);
        settingsButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        settingsButton.setStyle("-fx-background-color: rgba(50, 50, 50, 0.8); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 15 8 15;");
        settingsButton.layoutXProperty().bind(root.widthProperty().subtract(settingsButton.widthProperty()).subtract(30));
        settingsButton.setLayoutY(70);

        pauseMenu = new VBox(20);
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setStyle("-fx-background-color: rgba(30, 30, 30, 0.95); -fx-background-radius: 15; -fx-padding: 40; -fx-border-color: #4CAF50; -fx-border-width: 3; -fx-border-radius: 15;");
        pauseMenu.setMinWidth(400);

        pauseStatusLabel = new Label("");
        pauseStatusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        pauseStatusLabel.setTextFill(Color.WHITE);

        // Luôn khởi tạo pauseChancesLabel, nhưng tùy player2 để bật/tắt
        pauseChancesLabel = new Label("Lượt tạm dừng còn lại: 3");
        pauseChancesLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        pauseChancesLabel.setTextFill(Color.LIGHTGRAY);

        if (player2 == null) {
            pauseChancesLabel.setDisable(true);
            pauseChancesLabel.setVisible(false); // ẩn hẳn khỏi giao diện
        }

        pauseResumeButton = new Button("⏸ Tạm dừng");
        pauseResumeButton.setFocusTraversable(false);
        pauseResumeButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        pauseResumeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 12 30 12 30;");
        pauseResumeButton.setMinWidth(250);

        // Thêm nút thoát (đầu hàng)
        surrenderButton = new Button("🚪 Thoát");
        surrenderButton.setFocusTraversable(false);
        surrenderButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        surrenderButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 12 30 12 30;");
        surrenderButton.setMinWidth(250);
        surrenderButton.setOnAction(e -> {
            // Xác nhận người chơi có chắc chắn muốn thoát không
            if (player2 != null) {
                // Chế độ 2 người chơi: thoát = thua
                Client.getInstance().requestSurrender();
            } else {
                // Chế độ 1 người chơi: chỉ đơn giản thoát về menu
                gameLoop.stop();
                Main.getInstance().showMenuScene();
            }
        });

        pauseMenu.getChildren().addAll(pauseStatusLabel, pauseChancesLabel, pauseResumeButton, surrenderButton);

        pauseMenu.layoutXProperty().bind(root.widthProperty().subtract(pauseMenu.widthProperty()).divide(2));
        pauseMenu.layoutYProperty().bind(root.heightProperty().subtract(pauseMenu.heightProperty()).divide(2));
        pauseMenu.setVisible(false);

        settingsButton.setOnAction(e -> {
            boolean currentlyVisible = pauseMenu.isVisible();
            if (currentlyVisible) {
                clearPauseMenu();
            }
            pauseMenu.setVisible(!currentlyVisible);
        });

        // Xử lý logic nút tạm dừng
        if (player2 == null) {
            pauseResumeButton.setOnAction(e -> {
                if (gameLoop.isPaused()) {
                    gameLoop.resumeGame();
                    Client.getInstance().requestResumeGame();
                    pauseResumeButton.setText("⏸ Tạm dừng");
                    pauseStatusLabel.setText("");
                    pauseMenu.setVisible(false);
                    settingsButton.setDisable(false);
                } else {
                    gameLoop.pauseGame();
                    Client.getInstance().requestPauseGame();
                    pauseResumeButton.setText("▶ Tiếp tục");
                    pauseStatusLabel.setText("Trò chơi đã tạm dừng");
                    settingsButton.setDisable(true);
                }
            });
        } else {
            pauseResumeButton.setOnAction(e -> {
                if (gameLoop.isPaused()) {
                    Client.getInstance().requestResumeGame();
                } else {
                    Client.getInstance().requestPauseGame();
                }
            });
        }
    }

    // MỚI: Thiết lập UI chat
    private void setupChatUI(GameConfig config) {
        // Tạo nút toggle chat với khả năng kéo thả
        chatToggleButton = new Button("💬");
        chatToggleButton.setFocusTraversable(false);
        chatToggleButton.setFont(Font.font("Arial", 18));
        chatToggleButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand;");
        chatToggleButton.setLayoutX(10);
        chatToggleButton.setLayoutY(config.window.height - 50);
        
        // Thêm khả năng kéo thả cho nút toggle
        chatToggleButton.setOnMousePressed(event -> {
            dragOffsetX = event.getSceneX() - chatToggleButton.getLayoutX();
            dragOffsetY = event.getSceneY() - chatToggleButton.getLayoutY();
        });
        
        chatToggleButton.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - dragOffsetX;
            double newY = event.getSceneY() - dragOffsetY;
            
            // Giới hạn trong cửa sổ game
            newX = Math.max(0, Math.min(newX, config.window.width - chatToggleButton.getWidth()));
            newY = Math.max(0, Math.min(newY, config.window.height - chatToggleButton.getHeight()));
            
            chatToggleButton.setLayoutX(newX);
            chatToggleButton.setLayoutY(newY);
            
            // Cập nhật vị trí của chatBox theo nút toggle
            updateChatBoxPosition(config);
        });
        
        chatToggleButton.setOnMouseClicked(event -> {
            // Chỉ toggle chat nếu không phải là drag
            if (Math.abs(event.getSceneX() - (chatToggleButton.getLayoutX() + dragOffsetX)) < 5 &&
                Math.abs(event.getSceneY() - (chatToggleButton.getLayoutY() + dragOffsetY)) < 5) {
                toggleChat();
            }
        });
        
        // Container cho chat messages
        chatMessagesContainer = new VBox(5);
        chatMessagesContainer.setStyle("-fx-background-color: rgba(40, 40, 40, 0.5); -fx-padding: 10;");
        
        // ScrollPane cho chat messages
        chatScrollPane = new ScrollPane(chatMessagesContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setStyle("-fx-background: rgba(40, 40, 40, 0.5); -fx-background-color: transparent;");
        chatScrollPane.setPrefHeight(200);
        
        // Input field cho chat
        chatInput = new TextField();
        chatInput.setPromptText("Nhập tin nhắn...");
        chatInput.setFocusTraversable(false);
        chatInput.setStyle("-fx-background-color: rgba(85, 85, 85, 0.7); -fx-text-fill: white; -fx-prompt-text-fill: #999;");
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        
        // Nút gửi
        chatSendButton = new Button("Gửi");
        chatSendButton.setFocusTraversable(false);
        chatSendButton.setStyle("-fx-background-color: rgba(76, 175, 80, 0.8); -fx-text-fill: white;");
        
        // Container cho input và nút gửi
        HBox chatInputBox = new HBox(5, chatInput, chatSendButton);
        chatInputBox.setStyle("-fx-padding: 5;");
        
        // Container chính cho chat với độ trong suốt cao hơn
        chatBox = new VBox(5, chatScrollPane, chatInputBox);
        chatBox.setStyle("-fx-background-color: rgba(30, 30, 30, 0.5); -fx-background-radius: 10; -fx-padding: 5;");
        chatBox.setPrefWidth(300);
        chatBox.setMaxHeight(250);
        chatBox.setLayoutX(10);
        chatBox.setLayoutY(config.window.height - 310);
        chatBox.setVisible(false);
        
        // Event handlers
        chatSendButton.setOnAction(e -> sendChatMessage());
        chatInput.setOnAction(e -> sendChatMessage());
        
        // Thêm vào root
        root.getChildren().addAll(chatToggleButton, chatBox);
    }

    // MỚI: Cập nhật vị trí chatBox dựa trên vị trí của nút toggle
    private void updateChatBoxPosition(GameConfig config) {
        double buttonX = chatToggleButton.getLayoutX();
        double buttonY = chatToggleButton.getLayoutY();
        
        // Tính toán vị trí tốt nhất cho chatBox
        // Ưu tiên hiển thị chatBox ở phía trên nút toggle
        double chatBoxX = buttonX;
        double chatBoxY = buttonY - chatBox.getHeight() - 10; // 10px khoảng cách
        
        // Nếu không đủ chỗ ở trên, hiển thị ở dưới
        if (chatBoxY < 0) {
            chatBoxY = buttonY + chatToggleButton.getHeight() + 10;
        }
        
        // Đảm bảo chatBox không vượt ra ngoài cửa sổ
        chatBoxX = Math.max(0, Math.min(chatBoxX, config.window.width - chatBox.getWidth()));
        chatBoxY = Math.max(0, Math.min(chatBoxY, config.window.height - chatBox.getHeight()));
        
        chatBox.setLayoutX(chatBoxX);
        chatBox.setLayoutY(chatBoxY);
    }

    // Toggle hiển thị chat
    private void toggleChat() {
        isChatVisible = !isChatVisible;
        chatBox.setVisible(isChatVisible);
        if (isChatVisible) {
            // Cập nhật vị trí chatBox trước khi hiển thị
            GameConfig config = Main.getInstance().getGameConfig();
            updateChatBoxPosition(config);
            chatInput.requestFocus();
            // Reset số tin nhắn chưa đọc khi mở chat
            unreadMessageCount = 0;
            updateChatToggleButton();
        }
    }

    // Cập nhật nút toggle với số tin nhắn chưa đọc
    private void updateChatToggleButton() {
        if (unreadMessageCount > 0) {
            chatToggleButton.setText("💬 (" + unreadMessageCount + ")");
            chatToggleButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand;");
        } else {
            chatToggleButton.setText("💬");
            chatToggleButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand;");
        }
    }

    // Gửi tin nhắn chat
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            Client.getInstance().sendChatMessage(message);
            chatInput.clear();
        }
    }

    // Nhận và hiển thị tin nhắn chat
    public void receiveChat(String senderUsername, String message) {
        String myUsername = Client.getInstance().getUsername();
        
        // 1. Thêm tin nhắn vào chatbox
        Text senderText = new Text(senderUsername + ": ");
        senderText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text messageText = new Text(message + "\n");
        messageText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        
        // Màu sắc khác nhau cho người gửi và người nhận
        if (senderUsername.equals(myUsername)) {
            senderText.setFill(Color.LIGHTGREEN);
            messageText.setFill(Color.WHITE);
        } else {
            senderText.setFill(Color.LIGHTBLUE);
            messageText.setFill(Color.LIGHTGRAY);
        }
        
        TextFlow textFlow = new TextFlow(senderText, messageText);
        chatMessagesContainer.getChildren().add(textFlow);
        
        // Auto scroll xuống dưới cùng
        chatScrollPane.setVvalue(1.0);
        
        // 2. Hiển thị bong bóng chat tạm thời (chỉ với tin nhắn từ người khác)
        if (!senderUsername.equals(myUsername)) {
            showChatBubble(senderUsername, message);
            
            // Tăng số tin nhắn chưa đọc nếu chat đang đóng
            if (!isChatVisible) {
                unreadMessageCount++;
                updateChatToggleButton();
            }
        }
    }

    // Hiển thị bong bóng chat tự động biến mất
    private void showChatBubble(String senderUsername, String message) {
        // Tạo label cho bong bóng
        Label bubble = new Label(senderUsername + ": " + message);
        bubble.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        bubble.setTextFill(Color.WHITE);
        bubble.setStyle(
            "-fx-background-color: rgba(50, 50, 50, 0.9); " +
            "-fx-background-radius: 15; " +
            "-fx-padding: 10 15; " +
            "-fx-border-color: #4CAF50; " +
            "-fx-border-radius: 15; " +
            "-fx-border-width: 2;"
        );
        bubble.setMaxWidth(300);
        bubble.setWrapText(true);
        
        // Đặt vị trí bong bóng (góc trên bên trái, dưới score)
        bubble.setLayoutX(20);
        bubble.setLayoutY(50);
        
        // Thêm vào root
        root.getChildren().add(bubble);
        
        // Hiệu ứng fade in
        bubble.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), bubble);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Tự động biến mất sau 2 giây
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(event -> {
            // Hiệu ứng fade out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), bubble);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> root.getChildren().remove(bubble));
            fadeOut.play();
        });
        delay.play();
    }

    private void clearPauseMenu() {
        pauseStatusLabel.setText("");
        pauseChancesLabel.setText("");
    }

    public void updatePauseChancesDisplay(int chancesLeft, boolean isPauser) {
        if (isPauser) {
            pauseChancesLabel.setVisible(true);
            pauseChancesLabel.setText("Lượt tạm dừng còn lại: " + chancesLeft);
        } else {
            pauseChancesLabel.setVisible(false);
        }
    }

    public void handleGamePaused(String pauserUsername, String timeLeft, String chanceLeft) {
        String myUsername = Client.getInstance().getUsername();

        int time = Integer.parseInt(timeLeft);
        int chances = Integer.parseInt(chanceLeft);
        System.out.println("check_time_left: " + timeLeft);

        if (time == -1) {
            if (pauserUsername.equals(myUsername)) {
                pauseStatusLabel.setText("Bạn đã hết lượt tạm dừng!");
            } else {
                pauseStatusLabel.setText(pauserUsername + " đã hết lượt tạm dừng!");
            }
            updatePauseChancesDisplay(chances, false);
            return;
        }

        if (!gameLoop.isPaused()) {
            gameLoop.pauseGame();
            pauseMenu.setVisible(true);
            pauseResumeButton.setText("Tiếp tục");
            settingsButton.setDisable(true);
        }

        if (pauserUsername.equals(myUsername)) {
            pauseStatusLabel.setText("Bạn đã tạm dừng trò chơi (" + timeLeft + " giây còn lại)");
            updatePauseChancesDisplay(chances, true);
        } else {
            pauseStatusLabel.setText("Game đã được tạm dừng bởi " + pauserUsername + " (" + timeLeft + " giây còn lại)");
            updatePauseChancesDisplay(chances, false);
        }
    }

    public void handleGameResumed() {
        if (gameLoop.isPaused()) {
            gameLoop.resumeGame();
            pauseResumeButton.setText("Tạm dừng");
            pauseStatusLabel.setText("");
            pauseMenu.setVisible(false);
            settingsButton.setDisable(false);
        }
    }

    public void spawnTrash(int id, double x, double y, TrashType type, int imageIndex) {
        Trash newTrash = new Trash(id, x, y, type, imageIndex);
        trashList.add(newTrash);
        root.getChildren().add(newTrash.getView());
    }

    public void updateScores() {
        scoreLabel1.setText(String.format("%s: %d", player1.getUsername(), player1.getScore()));
        if (player2 != null) {
            scoreLabel2.setText(String.format("%s: %d", player2.getUsername(), player2.getScore()));
        }
    }

    public void updateOpponentPosition(String username, double x, double y) {
        System.out.printf("Check gamescene: %f %f", x, y);
        if (player2 != null && player2.getUsername().equals(username)) {
            player2.setPosition(x, y);
        }
    }

    public void updatePlayerScore(String username, int score) {
        Player player = getPlayerByName(username);
        if (player != null) {
            player.setScore(score);
            updateScores();
        }
    }

    public Player getPlayerByName(String name) {
        if (player1.getUsername().equals(name)) return player1;
        if (player2 != null && player2.getUsername().equals(name)) return player2;
        return null;
    }

    public Trash getTrashById(int id) {
        return trashList.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }
    
    public void removeTrash(int id) {
        Trash trash = getTrashById(id);
        if (trash != null) {
            // Xóa view khỏi root
            root.getChildren().remove(trash.getView());
            // Xóa khỏi danh sách
            trashList.remove(trash);
        }
    }

    public void updateTimer(int secondsLeft) {
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    public void showGameOver(String winnerName) {
        stopGameLoop();

        VBox gameOverPane = new VBox(20);
        gameOverPane.setAlignment(Pos.CENTER);
        gameOverPane.setStyle("-fx-background-color: rgba(20, 20, 20, 0.8); -fx-background-radius: 15;");

        Label gameOverLabel = new Label("GAME OVER");
        gameOverLabel.setFont(Font.font("Arial", FontWeight.BOLD, 52));
        gameOverLabel.setTextFill(Color.ORANGE);

        String resultMessage;

        // 1. Chế độ 1 người
        if (player2 == null) {
            if (playerLives <= 0) {
                resultMessage = "Bạn đã hết mạng!";
            } else {
                resultMessage = "Trò chơi kết thúc!";
            }
        }
        // 2. Chế độ 2 người
        else {
            if (winnerName.equalsIgnoreCase("TIE")) {
                resultMessage = "Kết quả: Hòa!";
            } else if (winnerName.equalsIgnoreCase(player1.getUsername())) {
                resultMessage = "Chúc mừng! Bạn đã thắng 🎉";
            } else {
                resultMessage = "Bạn đã thua 😢";
            }
        }

        Label winnerLabel = new Label(resultMessage);
        winnerLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 28));
        winnerLabel.setTextFill(Color.WHITE);

        Button backToMenuButton = new Button("Trở về Menu");
        backToMenuButton.setFocusTraversable(false);
        backToMenuButton.setFont(Font.font("Arial", 20));
        backToMenuButton.setOnAction(e -> {
            Client.getInstance().sendMessage("WAITING_OFF");
            Main.getInstance().showMenuScene();
        });

        gameOverPane.getChildren().addAll(gameOverLabel, winnerLabel, backToMenuButton);

        gameOverPane.layoutXProperty().bind(root.widthProperty().subtract(gameOverPane.widthProperty()).divide(2));
        gameOverPane.layoutYProperty().bind(root.heightProperty().subtract(gameOverPane.heightProperty()).divide(2));

        root.getChildren().add(gameOverPane);
    }

    public Scene getScene() {
        return scene;
    }

    public void startGameLoop() {
        gameLoop.start();
    }

    public void stopGameLoop() {
        gameLoop.stop();
    }
}