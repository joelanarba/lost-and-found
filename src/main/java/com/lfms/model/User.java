package com.lfms.model;

/**
 * A registered system user (STUDENT or ADMIN).
 */
public class User {

    private int userId;
    private String name;
    private String indexNo;
    private String email;
    private String passwordHash;
    private String phone;
    private String role;
    private boolean isActive;
    private String createdAt;

    public User() {
    }

    public User(int userId, String name, String indexNo, String email, String passwordHash,
                String phone, String role, boolean isActive, String createdAt) {
        this.userId = userId;
        this.name = name;
        this.indexNo = indexNo;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndexNo() {
        return indexNo;
    }

    public void setIndexNo(String indexNo) {
        this.indexNo = indexNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", name='" + name + '\'' + ", indexNo='" + indexNo + '\''
                + ", email='" + email + '\'' + ", role='" + role + '\'' + ", isActive=" + isActive + '}';
    }
}
