package com.auction.model;
public class Bidder extends User {
    private double balance;
    private double frozen; //tiền bị giữ khi đấu giá
    public Bidder(String name, String password, String email, String fullName, double balance) {
        super(name, password, email, fullName);
        this.balance = balance;
        this.frozen = 0;
    }
    @Override
    public void displayRoleInfo() {
        System.out.println("Role: BIDDER | Username: " + getUsername() + " | Balance: " + balance);
    }
    //getter
    public double getBalance() {
        return balance;
    }
    //setter
    public void addBalance(double amount) {
        balance += amount;
    }
    //khi đấu giá, trừ balance, cộng frozen
    public boolean freezeMoney(double amount) {
        if (balance < amount) return false;
        balance -= amount;
        frozen += amount;
        return true;
    }
    //khi có người thắng với bid cao hơn -> refund
    public void unfreezeMoney(double amount) {
        frozen -= amount;
        balance += amount;
    }
    //khi phiên kết thúc, thắng
    public void payForWonAuction(double amount) {
        if (frozen >= amount) {
            frozen -= amount;
        }
        else throw new IllegalStateException("Not enough frozen balance.");
    }
}
