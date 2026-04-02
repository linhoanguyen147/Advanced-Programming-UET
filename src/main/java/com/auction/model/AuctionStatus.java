package com.auction.model;
import java.time.LocalDateTime;
//trang thai phien dau gia
public enum AuctionStatus {
    OPEN, //đã tạo, chờ giờ start
    RUNNING, //đang diễn ra, cho phép bid
    FINISHED, //kết thúc, chờ thanh toán
    PAID, //đã thanh toán cho ng win
    CANCELED //bị huỷ (bởi admin hoặc seller)
}
