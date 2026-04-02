package com.auction.model;
import java.time.LocalDateTime;
import java.util.UUID;
public abstract class Entity {
    private String id;
    private LocalDateTime createdAt;
    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }
    //getter
    public String getId() {
        return this.id;
    }
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public void setId(String id) { this.id = id; }
}