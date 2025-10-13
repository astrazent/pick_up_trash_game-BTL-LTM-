package gamePlay.network;

import gamePlay.utils.DatabaseConnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientTCPHandler implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientTCPHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Nhận từ client " + clientSocket.getInetAddress() + ": " + inputLine);
                handleClientMessage(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Client đã ngắt kết nối: " + clientSocket.getInetAddress());
        } finally {
            server.removeClient(this); // Báo cho server biết client đã rời
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Phương thức để server gửi tin nhắn đến client
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void handleClientMessage(String message) {
        String[] parts = message.split(";");
        String command = parts[0];

        switch (command) {
            case "LOGIN":
                // Message format: "LOGIN;username;password"
                if (parts.length == 3) {
                    String user = parts[1];
                    String pass = parts[2];
                    if (DatabaseConnector.validateUser(user, pass)) {
                        this.username = user;
                        out.println("LOGIN_SUCCESS;" + user);
                    } else {
                        out.println("LOGIN_FAILED;Tên đăng nhập hoặc mật khẩu không đúng.");
                    }
                }
                break;

            case "READY":
                // Message format: "READY;username"
                if (parts.length == 2) {
                    server.playerIsReady(parts[1]);
                }
                break;

            case "SAVE_SCORE":
                // Message format: "SAVE_SCORE;username;score"
                if (parts.length == 3) {
                    DatabaseConnector.saveScore(parts[1], Integer.parseInt(parts[2]));
                }
                break;

            // Thêm các command khác ở đây (ví dụ: vào phòng chờ,...)
        }
    }
}
