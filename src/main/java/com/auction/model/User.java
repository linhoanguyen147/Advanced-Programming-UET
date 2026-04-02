package com.auction.model;
public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private boolean isActive; //state (admin can lock)
    public User(String username, String password, String email, String fullName) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.isActive = true;
    }
    public abstract void displayRoleInfo();
    //getter
    public String getUsername() {
        return this.username;
    }
    public String getFullName() {
        return this.fullName;
    }
    public String getEmail() {
        return this.email;
    }
    //setter
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean state) {
        isActive = state;
    }
    public boolean changePassword(String oldPassword, String newPassword) {
        if (oldPassword.equals(this.password)) {
            this.password = newPassword;
            return true;
        }
        return false;
    }
    public boolean verifyPassword(String password) {
        return this.password.equals(password);
    }
//    public void setPassword(String password) {
//        this.password = password;
//    }
}

