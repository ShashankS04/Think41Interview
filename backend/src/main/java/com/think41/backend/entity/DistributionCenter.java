package com.think41.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id; // Using @Id directly as CSV has unique IDs
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "distribution_centers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributionCenter {
    @Id
    private Long id; // Assuming 'id' from CSV is unique and can be used directly
    private String name;
    private Double latitude;
    private Double longitude;
}