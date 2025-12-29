package com.se300.store.model.unit;

import com.se300.store.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ModelUnitTest class contains unit tests for various models used in the
 * Smart Store application.
 * It includes tests for creation, basic operations, and validation of models
 * and enums utilized in the system.
 */
@DisplayName("Model Unit Tests")
public class ModelUnitTest {

    // ==================== USER MODEL ====================

    @Test
    @DisplayName("Test User model creation and getters/setters")
    public void testUserModel() {
        // Constructor with args
        User user = new User("john.doe@example.com", "password123", "John Doe");

        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals("John Doe", user.getName());

        // Update via setters
        user.setEmail("jane.doe@example.com");
        user.setPassword("newPass!");
        user.setName("Jane Doe");

        assertEquals("jane.doe@example.com", user.getEmail());
        assertEquals("newPass!", user.getPassword());
        assertEquals("Jane Doe", user.getName());
    }

    // ==================== PRODUCT MODEL ====================

    @Test
    @DisplayName("Test Product model creation and getters/setters")
    public void testProductModel() {
        Product product = new Product(
                "P001",
                "Milk",
                "Whole milk 1L",
                "1L",
                "Dairy",
                3.49,
                Temperature.refrigerated);

        assertEquals("P001", product.getId());
        assertEquals("Milk", product.getName());
        assertEquals("Whole milk 1L", product.getDescription());
        assertEquals("1L", product.getSize());
        assertEquals("Dairy", product.getCategory());
        assertEquals(3.49, product.getPrice());
        assertEquals(Temperature.refrigerated, product.getTemperature());

        // Update via setters
        product.setId("P002");
        product.setName("Chocolate Milk");
        product.setDescription("Chocolate milk 1L");
        product.setSize("1L Bottle");
        product.setCategory("Beverages");
        product.setPrice(4.25);
        product.setTemperature(Temperature.ambient);

        assertEquals("P002", product.getId());
        assertEquals("Chocolate Milk", product.getName());
        assertEquals("Chocolate milk 1L", product.getDescription());
        assertEquals("1L Bottle", product.getSize());
        assertEquals("Beverages", product.getCategory());
        assertEquals(4.25, product.getPrice());
        assertEquals(Temperature.ambient, product.getTemperature());
    }

    // ==================== CUSTOMER MODEL ====================

    @Test
    @DisplayName("Test Customer model creation and getters/setters")
    public void testCustomerModel() {
        Customer customer = new Customer(
                "C001",
                "Alice",
                "Smith",
                CustomerType.registered,
                "alice@example.com",
                "123 Main St");

        assertEquals("C001", customer.getId());
        assertEquals("Alice", customer.getFirstName());
        assertEquals("Smith", customer.getLastName());
        assertEquals(CustomerType.registered, customer.getType());
        assertEquals("alice@example.com", customer.getEmail());
        assertEquals("123 Main St", customer.getAccountAddress());
        assertNull(customer.getAgeGroup());
        assertNull(customer.getStoreLocation());
        assertNull(customer.getLastSeen());
        assertNull(customer.getBasket());

        // Set additional fields
        customer.setFirstName("Alicia");
        customer.setLastName("Johnson");
        customer.setType(CustomerType.guest);
        customer.setAccountAddress("456 Oak Ave");
        customer.setAgeGroup(CustomerAgeGroup.adult);

        StoreLocation location = new StoreLocation("S001", "A01");
        customer.setStoreLocation(location);

        Date now = new Date();
        customer.setLastSeen(now);

        Basket basket = new Basket("B001");
        customer.assignBasket(basket);

        assertEquals("Alicia", customer.getFirstName());
        assertEquals("Johnson", customer.getLastName());
        assertEquals(CustomerType.guest, customer.getType());
        assertEquals("456 Oak Ave", customer.getAccountAddress());
        assertEquals(CustomerAgeGroup.adult, customer.getAgeGroup());
        assertEquals(location, customer.getStoreLocation());
        assertEquals(now, customer.getLastSeen());
        assertEquals(basket, customer.getBasket());
    }

    // ==================== STORE MODEL ====================

    @Test
    @DisplayName("Test Store model creation and basic operations")
    public void testStoreModel() throws StoreException {
        Store store = new Store("S001", "789 Market St", "Flagship store");

        assertEquals("S001", store.getId());
        assertEquals("789 Market St", store.getAddress());
        assertEquals("Flagship store", store.getDescription());

        // Update fields
        store.setId("S002");
        store.setAddress("1010 Broadway");
        store.setDescription("Updated description");

        assertEquals("S002", store.getId());
        assertEquals("1010 Broadway", store.getAddress());
        assertEquals("Updated description", store.getDescription());

        // Basic operation: add aisle and retrieve it
        Aisle aisle = store.addAisle("A01", "Produce", "Fresh fruits and vegetables", AisleLocation.floor);

        assertNotNull(aisle);
        assertEquals("A01", aisle.getNumber());
        assertEquals("Produce", aisle.getName());

        // getAisle should return same aisle
        Aisle retrieved = store.getAisle("A01");
        assertEquals(aisle, retrieved);

        // Adding same aisle again should throw StoreException
        assertThrows(StoreException.class,
                () -> store.addAisle("A01", "Produce Duplicate", "Duplicate", AisleLocation.floor));
    }

    // ==================== BASKET MODEL ====================

    @Test
    @DisplayName("Test Basket model operations")
    public void testBasketModel() {
        Basket basket = new Basket("B001");
        assertEquals("B001", basket.getId());
        assertNull(basket.getCustomer());
        assertNull(basket.getStore());

        // Set customer and store references
        Customer customer = new Customer(
                "C001",
                "Bob",
                "Brown",
                CustomerType.guest,
                "bob@example.com",
                "111 Pine St");
        Store store = new Store("S001", "999 Test Rd", "Test Store");

        basket.setCustomer(customer);
        basket.setStore(store);

        assertEquals(customer, basket.getCustomer());
        assertEquals(store, basket.getStore());
    }

    // ==================== STORE LOCATION MODEL ====================

    @Test
    @DisplayName("Test StoreLocation model")
    public void testStoreLocationModel() {
        StoreLocation location = new StoreLocation("S001", "A05");

        assertEquals("S001", location.getStoreId());
        assertEquals("A05", location.getAisleId());

        location.setStoreId("S002");
        location.setAisleId("B10");

        assertEquals("S002", location.getStoreId());
        assertEquals("B10", location.getAisleId());

        String text = location.toString();
        assertTrue(text.contains("S002"));
        assertTrue(text.contains("B10"));
    }

    // ==================== ENUMS ====================

    @Test
    @DisplayName("Test Temperature enum")
    public void testTemperatureEnum() {
        Temperature[] values = Temperature.values();
        assertTrue(values.length >= 5);

        assertNotNull(Temperature.frozen);
        assertNotNull(Temperature.refrigerated);
        assertNotNull(Temperature.ambient);
        assertNotNull(Temperature.warm);
        assertNotNull(Temperature.hot);

        assertEquals(Temperature.frozen, Temperature.valueOf("frozen"));
    }

    @Test
    @DisplayName("Test CustomerType enum")
    public void testCustomerTypeEnum() {
        CustomerType[] values = CustomerType.values();
        assertEquals(2, values.length);
        assertEquals(CustomerType.guest, CustomerType.valueOf("guest"));
        assertEquals(CustomerType.registered, CustomerType.valueOf("registered"));
    }

    // ==================== STORE EXCEPTION ====================

    @Test
    @DisplayName("Test Store Exception")
    public void testStoreException() {
        StoreException ex = new StoreException("Add Product", "Not enough inventory");

        assertEquals("Add Product", ex.getAction());
        assertEquals("Not enough inventory", ex.getReason());

        ex.setAction("Remove Product");
        ex.setReason("Product Does Not Exist");

        assertEquals("Remove Product", ex.getAction());
        assertEquals("Product Does Not Exist", ex.getReason());
    }
}
