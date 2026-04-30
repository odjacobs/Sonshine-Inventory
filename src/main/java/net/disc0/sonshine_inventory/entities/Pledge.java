package net.disc0.sonshine_inventory.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pledges")
public class Pledge {
    public enum PledgeStatus {
        OPEN,
        FULFILLED,
        EXPIRED,
        CANCELLED
    }

    @Id
    @GeneratedValue
    private Integer id;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "donor_name", nullable = false)
    private String donorName;

    @Column(name = "donor_contact", nullable = false)
    private String donorContact;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PledgeStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Set to createdAt + 7 days in the application logic before saving
    @Column(name = "expires_at", nullable = false, insertable = true)
    private LocalDateTime expiresAt;

    @Column(name = "fulfilled_at", nullable = true)
    private LocalDateTime fulfilledAt;

    public Pledge() {}

    public Pledge(Integer itemId, String donorName, String donorContact, Integer quantity, PledgeStatus status, LocalDateTime expiresAt) {
        this.itemId = itemId;
        this.donorName = donorName;
        this.donorContact = donorContact;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorContact() {
        return donorContact;
    }

    public void setDonorContact(String donorContact) {
        this.donorContact = donorContact;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public PledgeStatus getStatus() {
        return status;
    }

    public void setStatus(PledgeStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(LocalDateTime fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }
}
