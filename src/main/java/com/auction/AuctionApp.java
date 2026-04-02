package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AuctionApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Tải file thiết kế giao diện FXML lên
        // (Chúng ta sẽ viết file main_view.fxml này ở bước sau)
        FXMLLoader fxmlLoader = new FXMLLoader(AuctionApp.class.getResource("/com/auction/view/main_view.fxml"));

        // Thiết lập khung hình (Rộng 800px, Cao 600px)
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Đặt tiêu đề cho cửa sổ
        stage.setTitle("Hệ thống Đấu giá Trực tuyến - Nhóm của Linh");
        stage.setScene(scene);
        stage.setResizable(false); // Khóa kích thước cửa sổ cho dễ thiết kế
        stage.show(); // Hiển thị cửa sổ
    }

    public static void main(String[] args) {
        // Lệnh kích hoạt giao diện JavaFX
        launch();
    }
}