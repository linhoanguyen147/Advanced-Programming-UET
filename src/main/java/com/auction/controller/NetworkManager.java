package com.auction.controller;

import com.auction.network.Message;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkManager {
    private static NetworkManager instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();

    // Tham chiếu đến Controller để gọi hàm cập nhật giao diện
    private MainController controller;

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) instance = new NetworkManager();
        return instance;
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }

    // Kết nối tới Server (Chạy ngầm trên một Thread khác)
    public void connect(String ip, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Platform.runLater(() -> controller.logToConsole("Đã kết nối thành công tới Server!"));

                // Vòng lặp liên tục lắng nghe Server
                String response;
                while ((response = in.readLine()) != null) {
                    Message msg = gson.fromJson(response, Message.class);
                    // Đẩy dữ liệu về luồng UI chính để xử lý an toàn
                    Platform.runLater(() -> controller.handleServerResponse(msg));
                }
            } catch (Exception e) {
                Platform.runLater(() -> controller.logToConsole("Lỗi kết nối hoặc mất mạng!"));
            }
        }).start();
    }

    // Gửi dữ liệu lên Server
    public void sendMessage(Message msg) {
        if (out != null) {
            out.println(gson.toJson(msg));
        }
    }
}