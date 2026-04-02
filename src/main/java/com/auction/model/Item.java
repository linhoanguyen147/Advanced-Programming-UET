package com.auction.model;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    private ItemStatus status;
    public Item(String name, String description, double startingPrice) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.status = ItemStatus.AVAILABLE;
    }
    public void setStatus(ItemStatus status) { this.status = status; }
    public ItemStatus getStatus() { return status; }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public abstract String getDetails();
    @Override
    public String toString() {
        return name + " (Starting Price: " + startingPrice + ")";
    }
}
