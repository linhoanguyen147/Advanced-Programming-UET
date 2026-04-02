package com.auction.network;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AuctionClient {
    private static final String SERVER_IP = "127.0.0.1"; // Localhost
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;

    public void start() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            gson = new Gson();

            System.out.println("Đã kết nối tới Server Đấu giá!");

            // 1. Khởi chạy luồng LẮNG NGHE Server (Realtime)
            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.start();

            // 2. Luồng chính: NHẬP LỆNH và GỬI đi (Dùng Scanner giả lập UI)
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Nhập lệnh (LOGIN/BID/EXIT): ");
                String command = scanner.nextLine();

                if (command.equalsIgnoreCase("EXIT")) {
                    break;
                } else if (command.equalsIgnoreCase("LOGIN")) {
                    // Tạo JSON thủ công hoặc dùng Object để test
                    String payload = "{\"username\":\"linh_buyer\", \"password\":\"123456\"}";
                    Message msg = new Message("LOGIN", payload);
                    out.println(gson.toJson(msg)); // Gửi lên server
                }
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Luồng chạy ngầm để nhận tin nhắn Realtime
    private void listenToServer() {
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                Message msg = gson.fromJson(serverResponse, Message.class);

                // Cập nhật giao diện (Mô phỏng in ra màn hình)
                System.out.println("\n[TIN NHẮN TỪ SERVER]: " + msg.getAction() + " - " + msg.getStatus() + " -> " + msg.getPayload());
                System.out.print("Nhập lệnh (LOGIN/BID/EXIT): "); // In lại prompt
            }
        } catch (Exception e) {
            System.out.println("Mất kết nối với Server.");
        }
    }

    public static void main(String[] args) {
        new AuctionClient().start();
    }
}