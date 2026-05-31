package com.lfms.model;

/**
 * A lost or found item report.
 *
 * <p>{@code type} is {@code LOST} or {@code FOUND}. {@code status} is one of
 * {@code OPEN}, {@code CLAIM_PENDING}, {@code APPROVED}, {@code RESOLVED}, {@code CLOSED}.</p>
 */
public class Item {

    public static final String TYPE_LOST  = "LOST";
    public static final String TYPE_FOUND = "FOUND";

    public static final String STATUS_OPEN          = "OPEN";
    public static final String STATUS_CLAIM_PENDING = "CLAIM_PENDING";
    public static final String STATUS_APPROVED      = "APPROVED";
    public static final String STATUS_RESOLVED      = "RESOLVED";
    public static final String STATUS_CLOSED        = "CLOSED";

    /** The fixed set of item categories offered throughout the UI. */
    public static final java.util.List<String> CATEGORIES = java.util.List.of(
            "Electronics", "Clothing", "Accessories", "Books & Stationery",
            "Keys", "ID/Cards", "Bags", "Other");

    private int itemId;
    private int userId;
    private String type;
    private String name;
    private String category;
    private String description;
    private String location;
    private String imagePath;
    private String dateReported;
    private String status;
    private String createdAt;

    public Item() {
    }

    public Item(int itemId, int userId, String type, String name, String category, String description,
                String location, String imagePath, String dateReported, String status, String createdAt) {
        this.itemId = itemId;
        this.userId = userId;
        this.type = type;
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.imagePath = imagePath;
        this.dateReported = dateReported;
        this.status = status;
        this.createdAt = createdAt;
    }

    public boolean isLost() {
        return TYPE_LOST.equalsIgnoreCase(type);
    }

    public boolean isFound() {
        return TYPE_FOUND.equalsIgnoreCase(type);
    }

    public boolean isOpen() {
        return STATUS_OPEN.equalsIgnoreCase(status);
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDateReported() {
        return dateReported;
    }

    public void setDateReported(String dateReported) {
        this.dateReported = dateReported;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Item{itemId=" + itemId + ", type='" + type + '\'' + ", name='" + name + '\''
                + ", category='" + category + '\'' + ", status='" + status + '\'' + '}';
    }
}
