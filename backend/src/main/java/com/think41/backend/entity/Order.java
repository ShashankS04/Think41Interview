package com.think41.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @Column(name = "order_id") // CSV column is order_id, so map it
    private Long id; // Renamed to id for JPA consistency, mapped to order_id column

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    private String status;
    private String gender; // This seems redundant if user has gender, but present in CSV
    @Column(name = "created_at")
    private LocalDate createdAt;
    @Column(name = "returned_at")
    private LocalDate returnedAt;
    @Column(name = "shipped_at")
    private LocalDate shippedAt;
    @Column(name = "delivered_at")
    private LocalDate deliveredAt;
    @Column(name = "num_of_item")
    private Integer numOfItem; // Ensure data type matches CSV
}