package com.think41.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    private Long id;
    private Double cost;
    private String category;
    private String name;
    private String brand;
    @Column(name = "retail_price")
    private Double retailPrice;
    private String department;
    private String sku;

    @ManyToOne // Many products can be associated with one distribution center
    @JoinColumn(name = "distribution_center_id", referencedColumnName = "id")
    private DistributionCenter distributionCenter;
}