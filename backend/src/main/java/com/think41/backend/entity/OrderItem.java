package com.think41.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id") // Map to 'order_id' column in 'orders' table
    private Order order;

    // Note: user_id is in order_items.csv, but it's typically derived from the Order's user.
    // If it's just for redundancy, you might not map it directly. If it represents
    // a specific user who *ordered this item within the order*, keep it.
    // For now, let's keep it as a direct column, but a ManyToOne to User is also possible.
    // A direct column user_id is fine here as it's denormalized from order.
    @Column(name = "user_id")
    private Long userId; // Keeping it as a simple ID for simplicity, as it's already in Order entity.

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    @OneToOne // Assuming an inventory item is unique to an order item
    @JoinColumn(name = "inventory_item_id", referencedColumnName = "id")
    private InventoryItem inventoryItem;

    private String status;
    @Column(name = "created_at")
    private LocalDate createdAt;
    @Column(name = "shipped_at")
    private LocalDate shippedAt;
    @Column(name = "delivered_at")
    private LocalDate deliveredAt;
    @Column(name = "returned_at")
    private LocalDate returnedAt;
}