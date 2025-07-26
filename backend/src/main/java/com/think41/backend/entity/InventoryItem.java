package com.think41.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    @Id
    private Long id;

    // Foreign Key to Product
    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    @Column(name = "created_at")
    private LocalDate createdAt;
    @Column(name = "sold_at")
    private LocalDate soldAt;
    private Double cost; // Cost of this specific inventory item instance

    // Note: The following fields (product_category, product_name, etc.) are denormalized.
    // It's often better to retrieve them via the 'product' relationship.
    // However, if your CSV explicitly provides them here and they might differ
    // from the 'products' table at the time of inventorying, you might keep them.
    // For simplicity, I'll map them as direct columns, but consider if this is
    // truly necessary for your data model, or if you can rely on 'Product' entity.
    // If you rely on Product, remove these fields and get them via inventoryItem.getProduct().getCategory() etc.
    @Column(name = "product_category")
    private String productCategory;
    @Column(name = "product_name")
    private String productName;
    @Column(name = "product_brand")
    private String productBrand;
    @Column(name = "product_retail_price")
    private Double productRetailPrice;
    @Column(name = "product_department")
    private String productDepartment;
    @Column(name = "product_sku")
    private String productSku;

    // Foreign Key to DistributionCenter
    @ManyToOne
    @JoinColumn(name = "product_distribution_center_id", referencedColumnName = "id")
    private DistributionCenter productDistributionCenter; // Represents where this specific inventory item is located
}