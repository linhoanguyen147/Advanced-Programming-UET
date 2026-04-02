package com.auction.network;
import com.auction.model.*;
import com.auction.service.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private static final int PORT = 8080;

    // Danh sách các Client đang kết nối an toàn với đa luồng
    private static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("=== KHỞI ĐỘNG HỆ THỐNG ĐẤU GIÁ SERVER ===");
        com.auction.service.DatabaseManager.initializeDatabase();
        // Khởi tạo dữ liệu mẫu (Mock Data) trước khi mở mạng
        initializeMockData();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang lắng nghe tại port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(">> Client mới kết nối: " + clientSocket.getInetAddress());

                // Khởi tạo Handler và đưa vào danh sách quản lý
                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm gọi khi Client ngắt kết nối để giải phóng bộ nhớ
    public static void removeClient(ClientHandler client) {
        connectedClients.remove(client);
        System.out.println(">> Đã ngắt kết nối một Client. Tổng số online: " + connectedClients.size());
    }

    private static void initializeMockData() {
        // Tích hợp UserManager và AuctionService tạo sẵn 1 vài user và item để test
        try {
            // Kiểm tra xem seller1 đã có trong DB chưa, nếu chưa thì đăng ký mới
            try {
                UserManager.getInstance().login("linh", "123");
            } catch (Exception e) {
                // Nếu login lỗi (chưa có tài khoản), thì tiến hành tạo mới
                UserManager.getInstance().registerSeller("linh", "123", "linh@mail.com", "Hoang Linh", "thegioididong");
            }

            Seller seller = (Seller) UserManager.getInstance().login("linh", "123");
            Item item = ItemFactory.createItem(ItemType.ELECTRONICS, "iPhone 15 Pro Max", "Mobile Phone", 10.0, "Apple");
            Auction mockAuction = new Auction(item, seller);
            mockAuction.setId("AUC-001");
            mockAuction.setStatus(AuctionStatus.RUNNING);
            AuctionService.getInstance().addAuction(mockAuction);

            System.out.println(">> Đã nạp phiên đấu giá mẫu: AUC-001");
        } catch (Exception e) {
            System.out.println(">> [LỖI MOCK DATA] " + e.getMessage());
        }
    }
}