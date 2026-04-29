package net.disc0.sonshine_inventory.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "unit_label", nullable = true)
    private String unitLabel;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "quota", nullable = true)
    private Integer quota;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public Item() {}

    public Item(Integer categoryId, String name, String unitLabel, Integer quantity, Integer quota, Boolean active) {
        this.categoryId = categoryId;
        this.name = name;
        this.unitLabel = unitLabel;
        this.quantity = quantity;
        this.quota = quota;
        this.active = active;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String unitLabel) {
        this.unitLabel = unitLabel;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
