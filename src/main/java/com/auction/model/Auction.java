package com.auction.model;

//prevent concurrency
import javax.swing.plaf.basic.BasicCheckBoxUI;
import java.time.LocalDateTime;
import java.util.AbstractCollection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.auction.service.*;

//an auction
public class Auction extends Entity {
    //định danh, liên kết
    private Item item;
    private Seller seller;
    //thời gian, trạng thái
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    //thông tin đấu giá
    private BidTransaction highestBid; //lich su dat gia max
    //CopyOnWriteArrayList đảm bảo thread-sàe khi có nhiều bid cùng lúc
    private final List<BidTransaction> bidHistory; //lich su dat gia an toan
    private final List<AuctionObserver> observers;

    public Auction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        if (item.getStatus() != ItemStatus.IN_AUCTION) {
            throw new IllegalArgumentException("Item is in another auction or sold");
        }
        this.item = item;
        this.item.setStatus(ItemStatus.IN_AUCTION); //khoá item lại
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
    }

    public Auction(Item item, Seller seller) {
        super();
        if (item.getStatus() == ItemStatus.IN_AUCTION) {
            throw new IllegalArgumentException("Item is in another auction or sold");
        }
        this.item = item;
        this.item.setStatus(ItemStatus.IN_AUCTION); //khoá item lại
        this.seller = seller;
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.of(2026,4,30,23,59);
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
    }

    public void addObserver(AuctionObserver observer) {
        //if (!observers.contains(observer))
        observers.add(observer);
    }
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }
    //notify for all viewer
    private void notify(BidTransaction newBid) {
        for (AuctionObserver ob : observers) {
            ob.newBidPlaced(this, newBid);
        }
    }
    //đặt giá cốt lõi (thread-safe)
    public synchronized void placeBid(Bidder bidder, double amount) throws Exception {
        if (this.status != AuctionStatus.RUNNING)
            throw new Exception("Phiên đấu giá đã kết thúc hoặc chưa bắt đầu!");

        double currentPrice = (highestBid != null) ? highestBid.getBidAmount() : item.getStartingPrice();

        if (amount <= currentPrice)
            throw new Exception("Giá đặt phải cao hơn giá hiện tại ($" + currentPrice + ")!");

        if (bidder.getBalance() < amount)
            throw new Exception("Tài khoản của bạn không đủ tiền! (Hiện có: $" + bidder.getBalance() + ")");

        // 1. Hoàn tiền cho người cũ (nếu có)
        if (highestBid != null) {
            Bidder oldBidder = highestBid.getBidder();
            oldBidder.unfreezeMoney(highestBid.getBidAmount());
            UserManager.getInstance().updateBalance(oldBidder.getUsername(), oldBidder.getBalance());
        }

        // 2. Trừ tiền người mới
        bidder.freezeMoney(amount);
        UserManager.getInstance().updateBalance(bidder.getUsername(), bidder.getBalance());

        // 3. CHỐT NGƯỜI DẪN ĐẦU MỚI
        this.highestBid = new BidTransaction(bidder, amount);
        this.bidHistory.add(highestBid);

        // 4. PHÁT THÔNG BÁO CHO TẤT CẢ AI ĐANG XEM
        for (AuctionObserver obs : observers) {
            obs.newBidPlaced(this, highestBid);
        }
    }
    //close auction (time ended)
    public synchronized void closeAuction() {
        if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.CANCELED || this.status == AuctionStatus.PAID) return;

        this.status = AuctionStatus.FINISHED;
        System.out.println("Auction " + this.getId() + " has been closed");
        Bidder winner = null;
        double finalPrice = 0;
        if (this.highestBid != null) {
            winner = this.highestBid.getBidder();
            finalPrice = this.highestBid.getBidAmount();

            winner.payForWonAuction(finalPrice);
            this.seller.addRevenue(finalPrice);
            this.item.setStatus(ItemStatus.SOLD);
            this.status = AuctionStatus.PAID;
        }
        else {
            //noone bid
            this.item.setStatus(ItemStatus.AVAILABLE);
        }
        for (AuctionObserver ob : observers) {
            ob.auctionEnded(this, winner, finalPrice);
        }
    }
    //cancel by Admin / Seller
    private synchronized void exeuteCancellation() {
        if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.CANCELED || this.status == AuctionStatus.PAID) return;
        this.status = AuctionStatus.CANCELED;
        if (this.highestBid != null) {
            Bidder leader = this.highestBid.getBidder();
            leader.unfreezeMoney(this.highestBid.getBidAmount());
        }
        this.item.setStatus(ItemStatus.AVAILABLE);
        for (AuctionObserver ob : observers) {
            ob.auctionEnded(this, null, 0);
        }
    }
    //for admin: no condition
    public void cancelByAdmin(Admin admin, String reason) {
        exeuteCancellation();
    }
    //for seller: check
    public void cancelBySeller(Seller seller, String reason) {
        if (!this.seller.getId().equals(seller.getId())) return;
        exeuteCancellation();
    }
    //getter
    public Item getItem() {
        return item;
    }
    public AuctionStatus getStatus() {
        return status;
    }

    public BidTransaction getHighestBid() {
        return highestBid;
    }
    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }
    //setter (for admin / auto)
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}
