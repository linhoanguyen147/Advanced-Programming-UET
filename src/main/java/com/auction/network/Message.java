package com.auction.network;

public class Message {
    private String action;      // Lệnh: LOGIN, PLACE_BID, EVENT_NEW_BID...
    private String status;      // Trạng thái: SUCCESS, ERROR
    private String payload;     // Dữ liệu thực tế (dưới dạng chuỗi JSON)

    // Dùng khi Client gửi Request lên Server
    public Message(String action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    // Dùng khi Server gửi Response về Client
    public Message(String action, String status, String payload) {
        this.action = action;
        this.status = status;
        this.payload = payload;
    }

    public String getAction() { return action; }
    public String getStatus() { return status; }
    public String getPayload() { return payload; }
}