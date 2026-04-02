//package com.auction.service;
//
//import com.auction.model.*;
//
//import java.util.*;
//
//public class UserManager {
//    private Map<String, User> userDatabase; //key = username, value = User
//    //singleton pattern: only 1 usermanager
//    private static UserManager instance;
//    private UserManager(){
//        userDatabase = new HashMap<>();
//    }
//    public static UserManager getInstance(){
//        if(instance == null){
//            instance = new UserManager();
//        }
//        return instance;
//    }
//    //REGISTRATION
//    // Thêm hàm này vào dưới hàm login()
//    public void registerBidder(String username, String password, String email, String fullName) {
//        // Kiểm tra xem user đã tồn tại chưa
//        if (userDatabase.containsKey(username)) {
//            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại! Vui lòng chọn tên khác.");
//        }
//
//        // Tạo tài khoản Bidder mới (tặng sẵn 10000 để đi đấu giá)
//        Bidder newBidder = new Bidder(username, password, email, fullName, 10000);
//        userDatabase.put(username, newBidder);
//
//        System.out.println("[SERVER] Đã tạo tài khoản mới: " + username);
//    }
//    public Seller registerSeller(String username, String password, String email, String fullName, String shopName){
//        if  (userDatabase.containsKey(username)) throw new IllegalArgumentException("Username is already in use");
//        Seller seller = new Seller(username, password, email, fullName, shopName);
//        userDatabase.put(username, seller);
//        System.out.println("Successfully registered Seller: " + username);
//        return seller;
//    }
//    //LOGIN
//    public User login(String username, String password){
//        User user = userDatabase.get(username);
//        if (user == null) throw new IllegalArgumentException("Cannot find user");
//        if (!user.verifyPassword(password)) throw new IllegalArgumentException("Wrong Password") ;
//        if (!user.isActive()) throw new IllegalArgumentException("User is not active");
//        System.out.println("Successfully logged in. Welcome: " + username);
//        return user;
//    }
//    public Map<String, User> getAllUsers(){ //for Admin
//        return userDatabase;
//    }
//}
package com.auction.service;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserManager {
    private static UserManager instance;

    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    // --- HÀM ĐĂNG NHẬP BẰNG SQL ---
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Lấy CẦN THẬN từng cột dữ liệu từ Database
                String email = rs.getString("email");
                String fullName = rs.getString("fullName");
                double balance = rs.getDouble("balance");
                String shopName = rs.getString("shopName");
                String role = rs.getString("role");

                // TRUYỀN VÀO ĐÚNG CONSTRUCTOR BẠN YÊU CẦU
                switch (role) {
                    case "BIDDER":
                        return new Bidder(username, password, email, fullName, balance);
                    case "SELLER":
                        // Nếu shopName bị null trong DB, để mặc định là "Chưa đặt tên"
                        String finalShopName = (shopName != null) ? shopName : "Chưa đặt tên";
                        return new Seller(username, password, email, fullName, finalShopName);
                    case "ADMIN":
                        return new Admin(username, password, email, fullName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Sai thông tin đăng nhập hoặc tài khoản không tồn tại!");
    }
    // --- HÀM ĐĂNG KÝ CHO NGƯỜI BÁN (SELLER) ---
    public void registerSeller(String username, String password, String email, String fullName, String shopName) {
        String checkSql = "SELECT username FROM users WHERE username = ?";

        // BỔ SUNG: Cột shopName vào câu lệnh INSERT
        String insertSql = "INSERT INTO users(username, password, email, fullName, balance, shopName, role) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Kiểm tra xem user đã tồn tại chưa
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                if (checkStmt.executeQuery().next()) {
                    throw new IllegalArgumentException("Tên đăng nhập đã tồn tại! Vui lòng chọn tên khác.");
                }
            }

            // 2. Lưu vào Database
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, email);
                pstmt.setString(4, fullName);
                pstmt.setDouble(5, 0.0);         // Seller khởi đầu với 0$ (hoặc bạn có thể cho 1000$ tùy luật chơi)
                pstmt.setString(6, shopName);    // Lưu tên cửa hàng
                pstmt.setString(7, "SELLER");    // Đánh dấu quyền là SELLER

                pstmt.executeUpdate();
                System.out.println(">> Đã lưu Seller mới vào Database: " + username + " (Cửa hàng: " + shopName + ")");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    // --- HÀM ĐĂNG KÝ BẰNG SQL ---
    public void registerBidder(String username, String password, String email, String fullName) {
        String checkSql = "SELECT username FROM users WHERE username = ?";

        // BỔ SUNG: Thêm cột email vào câu lệnh INSERT
        String insertSql = "INSERT INTO users(username, password, email, fullName, balance, role) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                if (checkStmt.executeQuery().next()) {
                    throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, email);       // Set tham số số 3 là email
                pstmt.setString(4, fullName);
                pstmt.setDouble(5, 1000.0);      // Tặng 1000$ lúc đăng ký
                pstmt.setString(6, "BIDDER");
                pstmt.executeUpdate();
                System.out.println(">> Đã lưu user mới vào Database: " + username);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    // --- HÀM CẬP NHẬT SỐ DƯ TÀI KHOẢN ---
    public void updateBalance(String username, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Gán giá trị vào dấu ? trong câu lệnh SQL
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, username);

            // Thực thi lệnh cập nhật
            pstmt.executeUpdate();

        } catch (Exception e) {
            System.out.println(">> [LỖI DATABASE] Không thể cập nhật số dư: " + e.getMessage());
        }
    }
}