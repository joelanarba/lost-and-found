package com.lfms.model;

/**
 * A user's claim on a found item.
 *
 * <p>Persisted columns map to the {@code claims} table. The remaining fields are
 * <em>joined</em> values populated by {@code ClaimDAO} for display (item name, claimant
 * contact, and the reporter/finder contact) and are not stored directly on this row.</p>
 */
public class Claim {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    // Persisted fields
    private int claimId;
    private int itemId;
    private int claimantId;
    private String featuresDesc;
    private String proofDesc;
    private String proofImage;
    private String status;
    private String adminNote;
    private String createdAt;

    // Joined / display fields (set by DAO)
    private String itemName;
    private String claimantName;
    private String claimantIndexNo;
    private String claimantEmail;
    private String claimantPhone;
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;

    public Claim() {
    }

    public Claim(int claimId, int itemId, int claimantId, String featuresDesc, String proofDesc, String proofImage,
                 String status, String adminNote, String createdAt) {
        this.claimId = claimId;
        this.itemId = itemId;
        this.claimantId = claimantId;
        this.featuresDesc = featuresDesc;
        this.proofDesc = proofDesc;
        this.proofImage = proofImage;
        this.status = status;
        this.adminNote = adminNote;
        this.createdAt = createdAt;
    }

    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getClaimantId() {
        return claimantId;
    }

    public void setClaimantId(int claimantId) {
        this.claimantId = claimantId;
    }

    public String getFeaturesDesc() {
        return featuresDesc;
    }

    public void setFeaturesDesc(String featuresDesc) {
        this.featuresDesc = featuresDesc;
    }

    public String getProofDesc() {
        return proofDesc;
    }

    public void setProofDesc(String proofDesc) {
        this.proofDesc = proofDesc;
    }

    public String getProofImage() {
        return proofImage;
    }

    public void setProofImage(String proofImage) {
        this.proofImage = proofImage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public String getClaimantIndexNo() {
        return claimantIndexNo;
    }

    public void setClaimantIndexNo(String claimantIndexNo) {
        this.claimantIndexNo = claimantIndexNo;
    }

    public String getClaimantEmail() {
        return claimantEmail;
    }

    public void setClaimantEmail(String claimantEmail) {
        this.claimantEmail = claimantEmail;
    }

    public String getClaimantPhone() {
        return claimantPhone;
    }

    public void setClaimantPhone(String claimantPhone) {
        this.claimantPhone = claimantPhone;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }

    public String getReporterPhone() {
        return reporterPhone;
    }

    public void setReporterPhone(String reporterPhone) {
        this.reporterPhone = reporterPhone;
    }

    @Override
    public String toString() {
        return "Claim{claimId=" + claimId + ", itemId=" + itemId + ", claimantId=" + claimantId
                + ", status='" + status + '\'' + '}';
    }
}
