package com.auction.model;

public class Vehicle extends Item {
    private String brand; //Hãng
    private int year; //Năm ra đời
    //private int mileage;
    public Vehicle(String name, String description, double startingPrice, String brand, int year) {
        super(name, description, startingPrice);
        this.brand = brand;
        this.year = year;
        //this.mileage = mileage;
    }
    //setter
    public void setYear(int year) {
        this.year = year;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }
    //public int getMileage() { return mileage; }
    //public void setMileage(int mileage) { this.mileage = mileage; }
    //getter
    public String getBrand() {
        return brand;
    }
    public int getYear() {
        return year;
    }
    @Override
    public String getDetails() {
        return "[Vehicle] " + getName() + " | Brand: " + brand + " | Year: " + year + " | Description: " + getDescription();
    }
}
