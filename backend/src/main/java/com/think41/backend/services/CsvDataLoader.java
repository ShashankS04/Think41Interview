package com.think41.backend.services;

import com.think41.backend.Repo.*;
import com.think41.backend.entity.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class CsvDataLoader implements CommandLineRunner {

    private final DistributionCenterRepository distributionCenterRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OrderItemRepository orderItemRepository;

    // Use a map to store entities already loaded for relationships
    private final Map<Long, DistributionCenter> distributionCenters = new HashMap<>();
    private final Map<Long, Product> products = new HashMap<>();
    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Order> orders = new HashMap<>();
    private final Map<Long, InventoryItem> inventoryItems = new HashMap<>();


    public CsvDataLoader(DistributionCenterRepository distributionCenterRepository,
                         ProductRepository productRepository,
                         UserRepository userRepository,
                         OrderRepository orderRepository,
                         InventoryItemRepository inventoryItemRepository,
                         OrderItemRepository orderItemRepository) {
        this.distributionCenterRepository = distributionCenterRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0 && userRepository.count() == 0) {
            System.out.println("Loading initial data from CSVs...");
            loadDistributionCenters();
            loadProducts();
            loadUsers();
            loadOrders();
            loadInventoryItems();
            loadOrderItems();
            System.out.println("Initial data loading complete.");
        } else {
            System.out.println("Database already contains data. Skipping CSV data loading.");
        }
    }

    // --- UPDATED parseTimestamp method ---
    // Define a list of common timestamp patterns to try
    // Order matters: try more specific patterns first.
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
            // Example: 2024-01-01 13:11:59.341450+00:00 (Your most recent problematic format)
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSXXX"),
            // Example: 2024-01-01 13:11:59+00:00 (The previous problematic format)
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"),
            // Example: 2024-01-01 13:11:59.341 (No offset, milliseconds)
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            // Example: 2024-01-01 13:11:59 (No offset, no fractional seconds)
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            // Fallback for ISO_OFFSET_DATE_TIME which might handle some variations
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
    };

    private LocalDate parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty() || timestamp.equalsIgnoreCase("NULL")) {
            return null; // Handle null, empty, or "NULL" strings
        }

        String trimmedTimestamp = timestamp.trim();

        // Attempt to parse with each formatter in sequence
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                // Try parsing as OffsetDateTime first, as your data includes offsets
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmedTimestamp, formatter);
                return offsetDateTime.toLocalDate(); // Extract and return LocalDate
            } catch (DateTimeParseException e) {
                // Try next formatter if current one fails
            }
        }

        // If none of the formatters work, log a warning and return null
        System.err.println("Warning: Could not parse timestamp '" + timestamp + "'. Tried all known formats.");
        return null;
    }

    // --- End of UPDATED parseTimestamp method ---


    private void loadDistributionCenters() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("data/distribution_centers.csv").getInputStream()))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader() // Assumes first row is header
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                DistributionCenter dc = new DistributionCenter();
                dc.setId(Long.parseLong(record.get("id")));
                dc.setName(record.get("name"));
                dc.setLatitude(Double.parseDouble(record.get("latitude")));
                dc.setLongitude(Double.parseDouble(record.get("longitude")));
                distributionCenterRepository.save(dc);
                distributionCenters.put(dc.getId(), dc); // Store for later lookup
            }
            System.out.println("Loaded " + distributionCenters.size() + " distribution centers.");
        } catch (Exception e) {
            System.err.println("Error loading distribution centers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadProducts() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("data/products.csv").getInputStream()))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                Product product = new Product();
                product.setId(Long.parseLong(record.get("id")));
                product.setCost(Double.parseDouble(record.get("cost")));
                product.setCategory(record.get("category"));
                product.setName(record.get("name"));
                product.setBrand(record.get("brand"));
                product.setRetailPrice(Double.parseDouble(record.get("retail_price")));
                product.setDepartment(record.get("department"));
                product.setSku(record.get("sku"));

                Long dcId = Long.parseLong(record.get("distribution_center_id"));
                product.setDistributionCenter(Optional.ofNullable(distributionCenters.get(dcId))
                        .orElseThrow(() -> new RuntimeException("DistributionCenter with ID " + dcId + " not found for product " + product.getId())));

                productRepository.save(product);
                products.put(product.getId(), product); // Store for later lookup
            }
            System.out.println("Loaded " + products.size() + " products.");
        } catch (Exception e) {
            System.err.println("Error loading products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("data/users.csv").getInputStream()))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                User user = new User();
                user.setId(Long.parseLong(record.get("id")));
                user.setFirstName(record.get("first_name"));
                user.setLastName(record.get("last_name"));
                user.setEmail(record.get("email"));
                user.setAge(Integer.parseInt(record.get("age")));
                user.setGender(record.get("gender"));
                user.setState(record.get("state"));
                user.setStreetAddress(record.get("street_address"));
                user.setPostalCode(record.get("postal_code"));
                user.setCity(record.get("city"));
                user.setCountry(record.get("country"));
                user.setLatitude(Double.parseDouble(record.get("latitude")));
                user.setLongitude(Double.parseDouble(record.get("longitude")));
                user.setTrafficSource(record.get("traffic_source"));
                user.setCreatedAt(parseTimestamp(record.get("created_at"))); // Uses updated parseTimestamp

                userRepository.save(user);
                users.put(user.getId(), user); // Store for later lookup
            }
            System.out.println("Loaded " + users.size() + " users.");
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadOrders() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("data/orders.csv").getInputStream()))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                Order order = new Order();
                order.setId(Long.parseLong(record.get("order_id")));

                Long userId = Long.parseLong(record.get("user_id"));
                order.setUser(Optional.ofNullable(users.get(userId))
                        .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found for order " + order.getId())));

                order.setStatus(record.get("status"));
                order.setGender(record.get("gender"));
                order.setCreatedAt(parseTimestamp(record.get("created_at"))); // Uses updated parseTimestamp
                order.setReturnedAt(parseTimestamp(record.get("returned_at"))); // Uses updated parseTimestamp
                order.setShippedAt(parseTimestamp(record.get("shipped_at"))); // Uses updated parseTimestamp
                order.setDeliveredAt(parseTimestamp(record.get("delivered_at"))); // Uses updated parseTimestamp
                order.setNumOfItem(Integer.parseInt(record.get("num_of_item")));

                orderRepository.save(order);
                orders.put(order.getId(), order); // Store for later lookup
            }
            System.out.println("Loaded " + orders.size() + " orders.");
        } catch (Exception e) {
            System.err.println("Error loading orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadInventoryItems() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("data/inventory_items.csv").getInputStream()))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                InventoryItem item = new InventoryItem();
                item.setId(Long.parseLong(record.get("id")));

                Long productId = Long.parseLong(record.get("product_id"));
                item.setProduct(Optional.ofNullable(products.get(productId))
                        .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " not found for inventory item " + item.getId())));

                item.setCreatedAt(parseTimestamp(record.get("created_at"))); // Uses updated parseTimestamp
                item.setSoldAt(parseTimestamp(record.get("sold_at"))); // Uses updated parseTimestamp
                item.setCost(Double.parseDouble(record.get("cost")));
                item.setProductCategory(record.get("product_category"));
                item.setProductName(record.get("product_name"));
                item.setProductBrand(record.get("product_brand"));
                item.setProductRetailPrice(Double.parseDouble(record.get("product_retail_price")));
                item.setProductDepartment(record.get("product_department"));
                item.setProductSku(record.get("product_sku"));

                String dcIdString = record.get("product_distribution_center_id");
                if (dcIdString != null && !dcIdString.trim().isEmpty()) {
                    Long dcId = Long.parseLong(dcIdString);
                    item.setProductDistributionCenter(Optional.ofNullable(distributionCenters.get(dcId))
                            .orElseThrow(() -> new RuntimeException("DistributionCenter with ID " + dcId + " not found for inventory item " + item.getId())));
                } else {
                    item.setProductDistributionCenter(null);
                }

                inventoryItemRepository.save(item);
                inventoryItems.put(item.getId(), item);
            }
            System.out.println("Loaded " + inventoryItems.size() + " inventory items.");
        } catch (Exception e) {
            System.err.println("Error loading inventory items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadOrderItems() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("data/order_items.csv").getInputStream()))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                OrderItem orderItem = new OrderItem();
                orderItem.setId(Long.parseLong(record.get("id")));

                Long orderId = Long.parseLong(record.get("order_id"));
                orderItem.setOrder(Optional.ofNullable(orders.get(orderId))
                        .orElseThrow(() -> new RuntimeException("Order with ID " + orderId + " not found for order item " + orderItem.getId())));

                orderItem.setUserId(Long.parseLong(record.get("user_id")));

                Long productId = Long.parseLong(record.get("product_id"));
                orderItem.setProduct(Optional.ofNullable(products.get(productId))
                        .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " not found for order item " + orderItem.getId())));

                Long inventoryItemId = Long.parseLong(record.get("inventory_item_id"));
                orderItem.setInventoryItem(Optional.ofNullable(inventoryItems.get(inventoryItemId))
                        .orElseThrow(() -> new RuntimeException("InventoryItem with ID " + inventoryItemId + " not found for order item " + orderItem.getId())));

                orderItem.setStatus(record.get("status"));
                orderItem.setCreatedAt(parseTimestamp(record.get("created_at"))); // Uses updated parseTimestamp
                orderItem.setShippedAt(parseTimestamp(record.get("shipped_at"))); // Uses updated parseTimestamp
                orderItem.setDeliveredAt(parseTimestamp(record.get("delivered_at"))); // Uses updated parseTimestamp
                orderItem.setReturnedAt(parseTimestamp(record.get("returned_at"))); // Uses updated parseTimestamp

                orderItemRepository.save(orderItem);
            }
            System.out.println("Loaded " + orderItemRepository.count() + " order items.");
        } catch (Exception e) {
            System.err.println("Error loading order items: " + e.getMessage());
            e.printStackTrace();
        }
    }
}