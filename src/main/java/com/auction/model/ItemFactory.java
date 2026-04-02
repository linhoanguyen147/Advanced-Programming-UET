package com.auction.model;

public class ItemFactory {
    public static Item createItem(ItemType type, String name, String description, double startingPrice, String...extra) {
        switch(type) {
            case ELECTRONICS:
                //extra[0] = brand, extra[1] = warrantyMonths
                String brand = extra.length > 0 ? extra[0] : "Unknown Brand";
                int warranty = extra.length > 1 ? Integer.parseInt(extra[1]) : 0;
                return new Electronics(name, description, startingPrice, brand, warranty);
            case ART:
                //extra[0] = artist, extra[1] = genre;
                String artist = extra.length > 1 ? extra[0] : "Unknown Artist";
                String genre = extra.length > 1 ? extra[1] : "Unknown Genre";
                return new Art(name, description, startingPrice, artist, genre);
            case VEHICLE:
                //extra[0] = brand, extra[1] = year
                String Brand = extra.length > 0 ? extra[0] : "Unknown Brand";
                int year = extra.length > 1 ? Integer.parseInt(extra[1]) : 2026;
                return new Vehicle(name, description, startingPrice, Brand, year);
            default:
                throw new IllegalArgumentException("Invalid Item");
        }
    }
}

//Item phone = ItemFactory.createItem(
//    ItemType.ELECTRONICS,
//    "iPhone 15 Pro",
//    "Mới 100%, nguyên seal",
//    999.0,
//    "Apple", "12" // Các tham số extra
//);
//System.out.println(phone.getDetails());