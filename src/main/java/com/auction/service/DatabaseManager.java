package com.auction.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Tự động tạo ra file auction.db trong thư mục dự án
    private static final String URL = "jdbc:sqlite:auction.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Hàm này chạy 1 lần khi khởi động Server để tạo bảng nếu chưa có
    public static void initializeDatabase() {
        // BỔ SUNG: Cột email và shopName
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "username TEXT PRIMARY KEY, "
                + "password TEXT NOT NULL, "
                + "email TEXT, "
                + "fullName TEXT NOT NULL, "
                + "balance REAL DEFAULT 0, "
                + "shopName TEXT, "
                + "role TEXT NOT NULL"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);

            // Cập nhật câu lệnh INSERT Admin mẫu có thêm email
            stmt.execute("INSERT OR IGNORE INTO users (username, password, email, fullName, balance, role) " +
                    "VALUES ('admin', '123', 'admin@hethong.com', 'Super Admin', 0, 'ADMIN')");

            System.out.println(">> Database đã sẵn sàng!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}