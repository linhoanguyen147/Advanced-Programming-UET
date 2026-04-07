package com.auction.controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.auction.network.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

public class MainController {
    @FXML private Button btnCreateAuction;
    // === CÁC THÀNH PHẦN GIAO DIỆN (Liên kết từ FXML qua fx:id) ===
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblUserStatus;

    // Quản lý Bảng danh sách phiên
    @FXML private TableView<AuctionViewItem> tableAuctions;
    @FXML private TableColumn<AuctionViewItem, String> colAuctionId;
    @FXML private TableColumn<AuctionViewItem, String> colItemName;
    @FXML private TableColumn<AuctionViewItem, String> colStatus;
    @FXML private Button btnRefresh;

    // Quản lý Khu vực Chi tiết & Đặt giá
    @FXML private Label lblItemName, lblItemDesc, lblStartingPrice;
    @FXML private Label lblHighestBid, lblHighestBidder;
    @FXML private TextArea txtRealtimeLog;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    // Quản lý Vùng Đăng nhập / Đăng xuất
    @FXML private HBox boxLogin;
    @FXML private HBox boxLoggedIn;
    @FXML private Label lblUserName;
    @FXML private Button btnRegister;
    @FXML private Button btnLogout;
    @FXML private Label lblBalance;

    private Gson gson = new Gson();
    private String currentAuctionId = ""; // Lưu lại ID phiên đang xem

    // === HÀM KHỞI TẠO (Chạy ngay khi giao diện vừa bật lên) ===
    @FXML
    public void initialize() {
        logToConsole("Đang khởi động hệ thống giao diện...");
        NetworkManager.getInstance().setController(this);
        NetworkManager.getInstance().connect("127.0.0.1", 8080);

        // --- CẤU HÌNH BẢNG (TABLE VIEW) ---
        // Ghép cột với thuộc tính trong class AuctionViewItem
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Lắng nghe sự kiện CLICK chuột vào một dòng trong bảng
        tableAuctions.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Lấy ID của phiên đấu giá đang được chọn
                currentAuctionId = newSelection.getId();

                // Cập nhật giao diện bên phải
                lblItemName.setText(newSelection.getName());
                lblItemDesc.setText("Đang tải chi tiết..."); // Thực tế sẽ lấy từ Server
                btnPlaceBid.setDisable(false); // Mở khóa nút đặt giá

                // Gửi lệnh WATCH lên Server để nhận thông báo Realtime cho phiên này
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", currentAuctionId);
                NetworkManager.getInstance().sendMessage(new Message("WATCH", payload.toString()));

                logToConsole("Đã chọn phiên: " + currentAuctionId);
            }
        });
    }

    // === BẮT SỰ KIỆN NÚT BẤM (Từ onAction trong FXML) ===

    @FXML
    void handleLogin(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        // Tạo JSON Payload và gửi lên Server
        JsonObject payload = new JsonObject();
        payload.addProperty("username", user);
        payload.addProperty("password", pass);

        NetworkManager.getInstance().sendMessage(new Message("LOGIN", payload.toString()));
        txtPassword.clear();
    }
    @FXML
    void handleShowRegister(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đăng ký tài khoản");
        dialog.setHeaderText("Vui lòng điền thông tin và chọn vai trò của bạn");

        // Các ô nhập liệu cơ bản
        TextField txtNewUser = new TextField(); txtNewUser.setPromptText("Tên đăng nhập");
        PasswordField txtNewPass = new PasswordField(); txtNewPass.setPromptText("Mật khẩu");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email");
        TextField txtFullName = new TextField(); txtFullName.setPromptText("Họ và Tên");

        // --- BỔ SUNG: CHỌN VAI TRÒ (ROLE) ---
        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton rbBidder = new RadioButton("Người mua (Bidder)");
        rbBidder.setToggleGroup(roleGroup);
        rbBidder.setSelected(true); // Mặc định là người mua

        RadioButton rbSeller = new RadioButton("Người bán (Seller)");
        rbSeller.setToggleGroup(roleGroup);

        HBox roleBox = new HBox(15, rbBidder, rbSeller); // Xếp 2 nút nằm ngang

        // Ô nhập tên cửa hàng (Mặc định bị khóa)
        TextField txtShopName = new TextField();
        txtShopName.setPromptText("Tên cửa hàng (Chỉ dành cho Người bán)");
        txtShopName.setDisable(true);

        // Lắng nghe sự kiện: Nếu chọn Người bán thì mở khóa ô nhập Tên cửa hàng
        roleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == rbSeller) {
                txtShopName.setDisable(false);
                txtShopName.requestFocus();
            } else {
                txtShopName.setDisable(true);
                txtShopName.clear();
            }
        });

        // Đưa tất cả vào khung dọc (VBox)
        VBox vbox = new VBox(10,
                new Label("Tên đăng nhập:"), txtNewUser,
                new Label("Mật khẩu:"), txtNewPass,
                new Label("Email:"), txtEmail,
                new Label("Họ và Tên:"), txtFullName,
                new Label("Bạn muốn tham gia với vai trò gì?"), roleBox,
                txtShopName
        );

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Bắt sự kiện khi bấm OK
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                JsonObject payload = new JsonObject();
                payload.addProperty("username", txtNewUser.getText());
                payload.addProperty("password", txtNewPass.getText());
                payload.addProperty("email", txtEmail.getText());
                payload.addProperty("fullName", txtFullName.getText());

                // Đóng gói Role và ShopName gửi xuống Server
                if (rbSeller.isSelected()) {
                    payload.addProperty("role", "SELLER");
                    payload.addProperty("shopName", txtShopName.getText());
                } else {
                    payload.addProperty("role", "BIDDER");
                }

                NetworkManager.getInstance().sendMessage(new Message("REGISTER", payload.toString()));
            }
        });
    }

    @FXML
    void handleLogout(ActionEvent event) {
        // 1. Gửi lệnh báo cho Server biết
        NetworkManager.getInstance().sendMessage(new Message("LOGOUT", "{}"));

        // 2. Chuyển đổi giao diện về trạng thái chưa đăng nhập
        boxLoggedIn.setVisible(false); boxLoggedIn.setManaged(false);
        boxLogin.setVisible(true); boxLogin.setManaged(true);

        // 3. Xóa các trường dữ liệu hiện tại
        txtPassword.clear();
        btnPlaceBid.setDisable(true);
        logToConsole("Đã đăng xuất an toàn.");
    }
    @FXML
    void handleRefreshAuctions(ActionEvent event) {
        logToConsole("Đang yêu cầu danh sách phiên đấu giá từ Server...");
        // Gửi lệnh không cần payload (gửi chuỗi JSON rỗng "{}")
        NetworkManager.getInstance().sendMessage(new Message("GET_ALL_AUCTIONS", "{}"));
    }

    @FXML
    void handlePlaceBid(ActionEvent event) {
        if (currentAuctionId.isEmpty()) {
            showAlert("Lỗi", "Vui lòng chọn một phiên đấu giá trước!");
            return;
        }

        try {
            double amount = Double.parseDouble(txtBidAmount.getText());

            JsonObject payload = new JsonObject();
            payload.addProperty("auctionId", currentAuctionId);
            payload.addProperty("amount", amount);

            NetworkManager.getInstance().sendMessage(new Message("BID", payload.toString()));
            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            showAlert("Lỗi Nhập Liệu", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    // === XỬ LÝ PHẢN HỒI TỪ SERVER (Chạy trên luồng UI an toàn) ===

    public void handleServerResponse(Message msg) {
        String action = msg.getAction();
        String status = msg.getStatus();
        String payload = msg.getPayload();

        switch (action) {
            case "LOGIN_RES":
                if (status.equals("SUCCESS")) {
                    // Dịch chuỗi payload thành JSON thay vì String thuần
                    JsonObject data = gson.fromJson(payload, JsonObject.class);

                    lblUserName.setText(data.get("fullName").getAsString());
                    lblBalance.setText("Số dư: " + data.get("balance").getAsDouble()); // Hiển thị số dư

                    boxLogin.setVisible(false); boxLogin.setManaged(false);
                    boxLoggedIn.setVisible(true); boxLoggedIn.setManaged(true);
                    String role = data.get("role").getAsString();
                    if (role.equals("SELLER")) {
                        btnCreateAuction.setVisible(true);
                        btnCreateAuction.setManaged(true);
                    } else {
                        btnCreateAuction.setVisible(false);
                        btnCreateAuction.setManaged(false);
                    }
                    logToConsole("Đăng nhập thành công!");
                } else {
                    showAlert("Đăng nhập thất bại", payload);
                }
                break;

            case "REGISTER_RES":
                if (status.equals("SUCCESS")) {
                    showAlert("Thành công", payload);
                    logToConsole("Hệ thống: " + payload);
                } else {
                    showAlert("Lỗi đăng ký", payload);
                }
                break;

            case "LOGOUT_RES":
                // Có thể bỏ trống vì ta đã xử lý ngay lúc bấm nút rồi
                break;

            case "BID_RES":
                // Bắt lỗi đỏ (như thiếu tiền, giá thấp...)
                if (status.equals("ERROR")) {
                    showAlert("Lỗi Đặt Giá", payload);
                }
                break;

            case "WATCH_RES":
                if (status.equals("SUCCESS")) {
                    // Dịch JSON Server vừa gửi ở Bước 1
                    JsonObject data = gson.fromJson(payload, JsonObject.class);

                    // Điền vào phần "Chi tiết sản phẩm"
                    lblItemName.setText(data.get("itemName").getAsString());
                    lblItemDesc.setText(data.get("itemDesc").getAsString());
                    lblStartingPrice.setText("$" + data.get("startingPrice").getAsDouble());

                    // Cập nhật "Giá cao nhất hiện tại"
                    double highestBid = data.get("highestBid").getAsDouble();
                    if (highestBid > 0) {
                        lblHighestBid.setText("$" + highestBid);
                        lblHighestBidder.setText(data.get("highestBidder").getAsString());
                    } else {
                        lblHighestBid.setText("$0.0");
                        lblHighestBidder.setText("Chưa có ai");
                    }

                    logToConsole("Đã vào phòng: " + data.get("itemName").getAsString());
                }
                break;
            case "CREATE_AUCTION_RES":
                if (status.equals("SUCCESS")) {
                    // Hiển thị thông báo thành công cho Seller
                    showAlert("Đăng bán thành công", payload);
                    logToConsole(">>> 🟢 MỚI: " + payload);

                    // Mẹo UX (Trải nghiệm người dùng):
                    // Tự động gọi lệnh làm mới danh sách để món hàng vừa tạo hiện lên bảng ngay lập tức
                    NetworkManager.getInstance().sendMessage(new Message("GET_ALL_AUCTIONS", "{}"));
                } else {
                    // Báo lỗi nếu nhập sai định dạng hoặc chưa đủ quyền
                    showAlert("Lỗi Đăng Bán", payload);
                }
                break;
            case "EVENT_BID":
                if (status.equals("SUCCESS")) {
                    JsonObject eventData = gson.fromJson(payload, JsonObject.class);
                    // Bóc tách JSON
                    String bidderName = eventData.get("bidderName").getAsString();
                    double newAmount = eventData.get("amount").getAsDouble();

                    // ÉP LÊN GIAO DIỆN CHÍNH XÁC
                    lblHighestBid.setText("$" + newAmount);
                    lblHighestBidder.setText(bidderName);

                    logToConsole(">>> 🔥 REALTIME: " + bidderName + " vừa vươn lên dẫn đầu với $" + newAmount);
                }
                break;

            case "EVENT_END":
                logToConsole(">>> KẾT THÚC: " + payload);
                btnPlaceBid.setDisable(true);
                break;

            case "ERROR":
                showAlert("Lỗi từ Server", payload);
                break;

            case "AUCTION_LIST_RES":
                if (status.equals("SUCCESS")) {
                    // 1. Dịch chuỗi payload thành một Mảng JSON
                    JsonArray jsonArray = gson.fromJson(payload, JsonArray.class);

                    // 2. Tạo một danh sách rỗng để chuẩn bị nhét vào bảng
                    ObservableList<AuctionViewItem> list = FXCollections.observableArrayList();

                    // 3. Vòng lặp bóc tách từng phần tử trong mảng JSON
                    for (JsonElement element : jsonArray) {
                        JsonObject obj = element.getAsJsonObject();
                        String id = obj.get("id").getAsString();
                        String name = obj.get("name").getAsString();
                        String auctionStatus = obj.get("status").getAsString();

                        // Khởi tạo Object giao diện và ném vào danh sách
                        list.add(new AuctionViewItem(id, name, auctionStatus));
                    }

                    // 4. Đổ dữ liệu vào bảng (TableView sẽ tự động cập nhật hiển thị)
                    tableAuctions.setItems(list);
                    logToConsole("Đã cập nhật danh sách gồm " + list.size() + " phiên đấu giá.");
                } else {
                    showAlert("Lỗi Tải Dữ Liệu", payload);
                }
                break;

            case "UPDATE_BALANCE":
                // Cập nhật lại số dư (bị trừ đi) ngay lập tức trên góc màn hình
                if (status.equals("SUCCESS")) {
                    JsonObject data = gson.fromJson(payload, JsonObject.class);
                    lblBalance.setText("Số dư: $" + data.get("balance").getAsDouble());
                }
                break;
            default:
                logToConsole("Server gửi: " + payload);
        }
    }

    // === CÁC HÀM TIỆN ÍCH (Utilities) ===

    // In thông báo ra cái ô đen đen (TextArea) trên giao diện
    public void logToConsole(String message) {
        txtRealtimeLog.appendText(message + "\n");
    }

    // Hiển thị Popup cảnh báo
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    @FXML
    void handleCreateAuction(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm bạn muốn đưa lên sàn");

        TextField txtName = new TextField(); txtName.setPromptText("Tên sản phẩm (VD: Laptop Dell)");
        TextField txtDesc = new TextField(); txtDesc.setPromptText("Mô tả chi tiết / Thương hiệu");
        TextField txtPrice = new TextField(); txtPrice.setPromptText("Giá khởi điểm ($)");
        TextField txtDuration = new TextField(); txtDuration.setPromptText("Thời gian đấu giá (tính bằng Giây, VD: 120)");

        VBox vbox = new VBox(10,
                new Label("Tên sản phẩm:"), txtName,
                new Label("Mô tả:"), txtDesc,
                new Label("Giá khởi điểm ($):"), txtPrice,
                new Label("Thời gian đấu giá (Giây):"), txtDuration
        );

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    JsonObject payload = new JsonObject();
                    payload.addProperty("itemName", txtName.getText());
                    payload.addProperty("itemDesc", txtDesc.getText());
                    payload.addProperty("startPrice", Double.parseDouble(txtPrice.getText()));
                    payload.addProperty("duration", Integer.parseInt(txtDuration.getText()));

                    NetworkManager.getInstance().sendMessage(new Message("CREATE_AUCTION", payload.toString()));
                } catch (Exception e) {
                    showAlert("Lỗi nhập liệu", "Vui lòng nhập đúng định dạng số cho Giá và Thời gian!");
                }
            }
        });
    }
    // === CLASS PHỤ TRỢ CHO BẢNG GIAO DIỆN ===
    public static class AuctionViewItem {
        private String id;
        private String name;
        private String status;

        public AuctionViewItem(String id, String name, String status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
    }
}