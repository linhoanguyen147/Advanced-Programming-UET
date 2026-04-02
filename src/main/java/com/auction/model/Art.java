package com.auction.model;
public class Art extends Item {
    private String artist; //nghệ sĩ
    private String genre; //thể loại
    public Art(String name, String description, double startingPrice, String artist, String genre) {
        super(name, description, startingPrice);
        this.artist = artist;
        this.genre = genre;
    }
    //getter
    public String getArtist() {
        return artist;
    }
    public String getGenre() {
        return genre;
    }
    @Override
    public String getDetails() {
        return "[Art] " + getName() + " | Artist: " + artist + " Art Genre: " + genre + " | Description: " + getDescription();
    }
}
