package com.auction.model;

public interface AuctionObserver {
    void newBidPlaced(Auction auction, BidTransaction newBid);
    void auctionEnded(Auction auction, Bidder winner, double finalPrice);
}
