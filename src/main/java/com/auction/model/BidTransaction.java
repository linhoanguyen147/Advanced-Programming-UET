package com.auction.model;
//luu tru thong tin cua 1 lan dat gia
import java.time.LocalDateTime;
//a bid transaction
public class BidTransaction extends Entity {
    private Bidder bidder;
    private double bidAmount;
    public BidTransaction(Bidder bidder, double bidAmount) {
        super(); //lay id, createdAt (tgian dat gia)
        this.bidder = bidder;
        this.bidAmount = bidAmount;
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getBidAmount() {
        return bidAmount;
    }

    @Override
    public String toString() {
        return "[" + getCreatedAt() + "]" + bidder.getUsername() + " bid: " + bidAmount;
    }
}
