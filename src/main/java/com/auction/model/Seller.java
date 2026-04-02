package com.auction.model;
import java.util.ArrayList;
import java.util.List;
public class Seller extends User {
    private String shopName;
    private List<Item> myItems; // các sản phẩm
    private double revenue;
    public Seller(String username, String password, String email, String fullName, String shopName) {
        super(username, password, email, fullName);
        this.shopName = shopName;
        this.myItems = new ArrayList<>();
    }
    //setter
    public void addItem(Item item) {
        myItems.add(item);
        System.out.println("Added " + item.getName());
    }
    public boolean removeItem(Item item) {
        if (myItems.remove(item)) {
            System.out.println("Removed " + item.getName());
            return true;
        }
        return false;
    }
    public void addRevenue(double amount) {
        revenue += amount;
        System.out.println("Shop " + shopName + " received revenue of " + amount);
    }
    //getter
    public String getShopName() {
        return shopName;
    }
    public List<Item> getMyItems() { //get list of items for displaying (GUI)
        return myItems;
    }
    public void cancelAuction(Auction auction, String reason) {
        auction.cancelBySeller(this, reason);
    }
    @Override
    public void displayRoleInfo() {
        System.out.println("Role: SELLER | Shop: " + shopName);
    }
}