package com.auction.model;
public class Admin extends User{
    //private String department; //phòng ban, phan quyen quan tri
    public Admin(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        //this.department = department;
    }

    public void banUser(User user, String reason) {
        if (!user.isActive()) {
            System.out.println("Not active already.");
            return;
        }
        user.setActive(false);
        System.out.println("Admin [" + this.getUsername() + "] banned user: " + user.getUsername() + ". Reason: " + reason);
    }
    public void cancelAuction(Auction auction, String reason) {
        auction.cancelByAdmin(this, reason);
    }
    @Override
    public void displayRoleInfo() {
        System.out.println("Role: ADMIN | Username: " + getUsername());
    }
}
