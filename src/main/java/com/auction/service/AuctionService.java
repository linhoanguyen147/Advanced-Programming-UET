package com.auction.service;

import com.auction.model.Auction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private static AuctionService instance;

    // Dùng ConcurrentHashMap để Thread-safe khi nhiều Client truy cập cùng lúc
    private Map<String, Auction> activeAuctions;

    private AuctionService() {
        activeAuctions = new ConcurrentHashMap<>();
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    public Auction getAuctionById(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    public Map<String, Auction> getAllAuctions() {
        return activeAuctions;
    }
}