package com.auction.network;
import com.google.gson.JsonArray;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.auction.model.*;
import com.auction.service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Triển khai luôn AuctionObserver để Client này có thể "ngồi xem" đấu giá
public class ClientHandler implements Runnable, AuctionObserver {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;

    private User loggedInUser = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Message request = gson.fromJson(inputLine, Message.class);
                handleRouter(request);
            }
        } catch (Exception e) {
            System.out.println("Lỗi kết nối hoặc Client tự thoát.");
        } finally {
            // Khi Client tắt app, tự động xóa họ khỏi hệ thống
            cleanUp();
        }
    }

    // ==========================================
    // BỘ ĐỊNH TUYẾN (ROUTER) XỬ LÝ LỆNH TỪ CLIENT
    // ==========================================
    private void handleRouter(Message req) {
        try {
            String action = req.getAction();

            if (action.equals("LOGIN")) {
                JsonObject json = gson.fromJson(req.getPayload(), JsonObject.class);
                loggedInUser = UserManager.getInstance().login(json.get("username").getAsString(), json.get("password").getAsString());

                // BỔ SUNG: Gom dữ liệu trả về thành JSON
                JsonObject resData = new JsonObject();
                resData.addProperty("fullName", loggedInUser.getFullName());

                if (loggedInUser instanceof Bidder) {
                    resData.addProperty("balance", ((Bidder) loggedInUser).getBalance());
                    resData.addProperty("role", "BIDDER");
                }
                else if (loggedInUser instanceof Seller) {
                    resData.addProperty("balance", ((Seller) loggedInUser).getRevenue());
                    resData.addProperty("role", "SELLER"); // Thêm dòng này
                }
                else {
                    resData.addProperty("balance", 0.0); // Nếu là Admin
                }

                sendMessage(new Message("LOGIN_RES", "SUCCESS", resData.toString()));
            }
            else if (action.equals("CREATE_AUCTION")) {
                try {
                    // 1. Bảo vệ: Chỉ Seller mới được tạo
                    if (loggedInUser == null || !(loggedInUser instanceof Seller)) {
                        throw new Exception("Chỉ Người bán (Seller) mới có quyền đăng bán!");
                    }

                    // 2. Lấy dữ liệu từ giao diện
                    JsonObject json = gson.fromJson(req.getPayload(), JsonObject.class);
                    String itemName = json.get("itemName").getAsString();
                    String itemDesc = json.get("itemDesc").getAsString();
                    double startPrice = json.get("startPrice").getAsDouble();
                    int duration = json.get("duration").getAsInt();

                    // 3. Khởi tạo đối tượng
                    Seller seller = (Seller) loggedInUser;
                    Item item = ItemFactory.createItem(ItemType.ELECTRONICS, itemName, itemDesc, startPrice, itemDesc);

                    // Sử dụng Constructor có thời gian (duration) đã tạo ở bước trước
                    Auction newAuction = new Auction(item, seller, duration);

                    // Tự động sinh ID duy nhất cho phiên đấu giá (Ví dụ: AUC-1704123456)
                    newAuction.setId("AUC-" + System.currentTimeMillis() % 1000000);

                    // 4. Lưu vào hệ thống
                    AuctionService.getInstance().addAuction(newAuction);

                    // 5. Báo thành công
                    sendMessage(new Message("CREATE_AUCTION_RES", "SUCCESS", "Sản phẩm " + itemName + " đã được lên sàn!"));

                } catch (Exception e) {
                    sendMessage(new Message("CREATE_AUCTION_RES", "ERROR", e.getMessage()));
                }
            }
            else if (action.equals("WATCH")) {
                JsonObject json = gson.fromJson(req.getPayload(), JsonObject.class);
                String auctionId = json.get("auctionId").getAsString();

                Auction a = AuctionService.getInstance().getAuctionById(auctionId);
                if (a == null) {
                    sendMessage(new Message("ERROR", "FAIL", "Không tìm thấy phiên đấu giá!"));
                    return;
                }

                a.addObserver(this); // Đăng ký xem realtime

                // BỔ SUNG: Gom toàn bộ thông tin chi tiết gửi về Client
                JsonObject resData = new JsonObject();
                resData.addProperty("itemName", a.getItem().getName());
                resData.addProperty("itemDesc", a.getItem().getDetails());
                resData.addProperty("startingPrice", a.getItem().getStartingPrice());

                // Kiểm tra xem đã có ai đặt giá chưa
                if (a.getHighestBid() != null) {
                    resData.addProperty("highestBid", a.getHighestBid().getBidAmount());
                    resData.addProperty("highestBidder", a.getHighestBid().getBidder().getUsername());
                } else {
                    resData.addProperty("highestBid", 0.0);
                    resData.addProperty("highestBidder", "Chưa có ai");
                }

                sendMessage(new Message("WATCH_RES", "SUCCESS", resData.toString()));
            }
            else if (action.equals("BID")) {
                try {
                    JsonObject json = gson.fromJson(req.getPayload(), JsonObject.class);
                    String auctionId = json.get("auctionId").getAsString();
                    double amount = json.get("amount").getAsDouble();

                    Auction a = AuctionService.getInstance().getAuctionById(auctionId);
                    if (a == null) throw new Exception("Không tìm thấy phiên đấu giá!");

                    // Gọi lõi logic (Nếu lỗi sẽ văng xuống catch)
                    a.placeBid((Bidder) loggedInUser, amount);

                    // Gửi tin nhắn cập nhật số dư hiển thị cho người vừa đặt
                    JsonObject resData = new JsonObject();
                    resData.addProperty("balance", ((Bidder) loggedInUser).getBalance());
                    sendMessage(new Message("UPDATE_BALANCE", "SUCCESS", resData.toString()));

                } catch (Exception e) {
                    sendMessage(new Message("BID_RES", "ERROR", e.getMessage()));
                }
            }
            // ==========================================
            // NHÁNH XỬ LÝ ĐĂNG KÝ VÀ ĐĂNG XUẤT NẰM Ở ĐÂY
            // ==========================================
            else if (action.equals("REGISTER")) {
                JsonObject json = gson.fromJson(req.getPayload(), JsonObject.class);
                String user = json.get("username").getAsString();
                String pass = json.get("password").getAsString();
                String name = json.get("fullName").getAsString();

                // Trích xuất thêm email và role
                String email = json.has("email") ? json.get("email").getAsString() : user + "@mail.com";
                String role = json.has("role") ? json.get("role").getAsString() : "BIDDER";

                // Phân luồng lưu vào Database
                if (role.equals("SELLER")) {
                    // Nếu là người bán, lấy thêm tên cửa hàng
                    String shopName = json.has("shopName") ? json.get("shopName").getAsString() : "Shop chưa đặt tên";
                    UserManager.getInstance().registerSeller(user, pass, email, name, shopName);
                } else {
                    // Nếu là người mua bình thường
                    UserManager.getInstance().registerBidder(user, pass, email, name);
                }

                sendMessage(new Message("REGISTER_RES", "SUCCESS", "Đăng ký thành công! Hãy đăng nhập."));
            }
            else if (action.equals("LOGOUT")) {
                this.loggedInUser = null;
                sendMessage(new Message("LOGOUT_RES", "SUCCESS", "Đã đăng xuất khỏi máy chủ."));
            }
            else if (action.equals("GET_ALL_AUCTIONS")) {
                // 1. Lấy toàn bộ phiên đấu giá từ Service
                Collection<Auction> auctions = AuctionService.getInstance().getAllAuctions().values();

                // 2. Tạo một mảng JSON (JsonArray)
                JsonArray jsonArray = new JsonArray();
                for (Auction a : auctions) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", a.getId());
                    obj.addProperty("name", a.getItem().getName());
                    obj.addProperty("status", a.getStatus().toString());
                    jsonArray.add(obj); // Nhét từng món vào mảng
                }

                // 3. Gửi mảng JSON đó về cho Client
                sendMessage(new Message("AUCTION_LIST_RES", "SUCCESS", jsonArray.toString()));
            }
            else {
                // Nếu không lọt vào các if trên thì mới báo Lệnh không hợp lệ
                sendMessage(new Message("ERROR", "FAIL", "Lệnh không hợp lệ: " + action));
            }
        } catch (Exception e) {
            sendMessage(new Message("ERROR", "FAIL", e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống không xác định!"));
        }
    }

    // --- LOGIC CHI TIẾT CÁC LỆNH ---

    private void processLogin(String payload) {
        JsonObject jsonInfo = gson.fromJson(payload, JsonObject.class);
        String username = jsonInfo.get("username").getAsString();
        String password = jsonInfo.get("password").getAsString();

        this.loggedInUser = UserManager.getInstance().login(username, password);
        sendMessage(new Message("LOGIN_RESPONSE", "SUCCESS", "Đăng nhập thành công với tài khoản: " + username));
    }

    private void processWatchAuction(String payload) {
        JsonObject jsonInfo = gson.fromJson(payload, JsonObject.class);
        String auctionId = jsonInfo.get("auctionId").getAsString();

        Auction auction = AuctionService.getInstance().getAuctionById(auctionId);
        if (auction == null) throw new IllegalArgumentException("Không tìm thấy phiên đấu giá này!");

        // Đăng ký Client này vào danh sách Observer của phiên đấu giá
        auction.addObserver(this);
        sendMessage(new Message("WATCH_RESPONSE", "SUCCESS", "Đã tham gia phòng đấu giá: " + auction.getItem().getName()));
    }

    private void processBid(String payload) throws Exception {
        try {
            if (loggedInUser == null || !(loggedInUser instanceof Bidder)) {
                throw new SecurityException("Bạn chưa đăng nhập hoặc không có quyền đặt giá!");
            }

            JsonObject jsonInfo = gson.fromJson(payload, JsonObject.class);
            String auctionId = jsonInfo.get("auctionId").getAsString();
            double amount = jsonInfo.get("amount").getAsDouble();

            Auction auction = AuctionService.getInstance().getAuctionById(auctionId);
            if (auction == null) throw new IllegalArgumentException("Không tìm thấy phiên đấu giá!");

            Bidder currentBidder = (Bidder) loggedInUser;
            auction.placeBid(currentBidder, amount);

            // 5. Nếu chạy lọt xuống được dòng này nghĩa là ĐẶT GIÁ THÀNH CÔNG
            // Ta sẽ gửi tin nhắn cập nhật lại số dư mới nhất cho màn hình của người đặt
            JsonObject resData = new JsonObject();
            resData.addProperty("balance", currentBidder.getBalance());
            sendMessage(new Message("UPDATE_BALANCE", "SUCCESS", resData.toString()));
        }
        catch (Exception e) {
            // BẮT TRỌN LỖI: Thiếu tiền, giá thấp, chưa đăng nhập...
            // Gửi thông báo màu đỏ về cho giao diện (Lưu ý: dùng "BID_RES" để khớp với Client)
            sendMessage(new Message("BID_RES", "ERROR", e.getMessage()));
        }
    }

    // ==========================================
    // OBSERVER PATTERN: TỰ ĐỘNG GỬI SỰ KIỆN CHO CLIENT
    // ==========================================
    @Override
    public void onTimeTick(Auction auction, int timeLeft) {
        JsonObject data = new JsonObject();
        data.addProperty("timeLeft", timeLeft);
        sendMessage(new Message("TIME_UPDATE", "SUCCESS", data.toString()));
    }
    @Override
    public void newBidPlaced(Auction auction, BidTransaction newBid) {
        JsonObject eventData = new JsonObject();
        eventData.addProperty("auctionId", auction.getId());
        eventData.addProperty("bidderName", newBid.getBidder().getUsername());
        eventData.addProperty("amount", newBid.getBidAmount());

        sendMessage(new Message("EVENT_BID", "SUCCESS", eventData.toString()));

    }

    @Override
    public void auctionEnded(Auction auction, Bidder winner, double finalPrice) {
        JsonObject eventData = new JsonObject();
        eventData.addProperty("auctionId", auction.getId());
        eventData.addProperty("winner", winner != null ? winner.getUsername() : "NONE");
        eventData.addProperty("finalPrice", finalPrice);

        sendMessage(new Message("EVENT_END", "SUCCESS", eventData.toString()));
    }

    // ==========================================
    // UTILS (TIỆN ÍCH)
    // ==========================================
    public void sendMessage(Message message) {
        if (out != null) {
            out.println(gson.toJson(message));
        }
    }

    private void cleanUp() {
        AuctionServer.removeClient(this);
        // Trong thực tế, bạn sẽ cần lặp qua tất cả Auction và gọi removeObserver(this) để tránh rò rỉ bộ nhớ
    }
}