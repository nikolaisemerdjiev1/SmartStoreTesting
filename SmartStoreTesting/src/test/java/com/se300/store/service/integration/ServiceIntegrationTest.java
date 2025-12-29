package com.se300.store.service.integration;

import com.se300.store.data.DataManager;
import com.se300.store.model.*;
import com.se300.store.repository.StoreRepository;
import com.se300.store.repository.UserRepository;
import com.se300.store.service.AuthenticationService;
import com.se300.store.service.StoreService;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains integration tests for verifying the correct functionality
 * of various service workflows in the Smart Store system.
 */
@DisplayName("Service Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceIntegrationTest {

    // Integration tests for the Smart Store Services

    private static StoreService storeService;
    private static AuthenticationService authenticationService;
    private static DataManager dataManager;

    @BeforeAll
    public static void setUpClass() {
        dataManager = DataManager.getInstance();
    }

    @BeforeEach
    public void setUp() {
        // Reset all in-memory state before each test
        dataManager.clear();

        // Ensure clean, empty maps for stores and users so tests start
        // without any pre-populated data (e.g., default admin/user accounts).
        dataManager.put("stores", new java.util.HashMap<String, Store>());
        dataManager.put("users", new java.util.HashMap<String, User>());

        StoreRepository storeRepository = new StoreRepository(dataManager);
        UserRepository userRepository = new UserRepository(dataManager);

        storeService = new StoreService(storeRepository);
        authenticationService = new AuthenticationService(userRepository);

        // Clear static maps inside StoreService
        StoreService.clearAllMaps();
    }

    // =========================================================
    // 1) COMPLETE STORE WORKFLOW
    // =========================================================

    @Test
    @Order(1)
    @DisplayName("Integration: Complete Store workflow - provision, show, update, delete")
    public void testCompleteStoreWorkflow() throws StoreException {
        String token = "token";
        String storeId = "STORE-INT-1";

        // Provision
        Store created = storeService.provisionStore(storeId, "Integration Store", "123 Main St", token);
        assertNotNull(created, "Store should be created");
        assertEquals(storeId, created.getId());
        assertEquals("123 Main St", created.getAddress());
        assertEquals("Integration Store", created.getDescription());

        // Show
        Store fetched = storeService.showStore(storeId, token);
        assertNotNull(fetched, "Fetched store should not be null");
        assertEquals(storeId, fetched.getId());

        // Update
        Store updated = storeService.updateStore(storeId, "Updated Store", "456 New Ave");
        assertNotNull(updated);
        assertEquals("Updated Store", updated.getDescription());
        assertEquals("456 New Ave", updated.getAddress());

        // Get all
        Collection<Store> stores = storeService.getAllStores();
        assertEquals(1, stores.size(), "There should be exactly one store after update");

        // Delete
        storeService.deleteStore(storeId);
        assertTrue(storeService.getAllStores().isEmpty(), "No stores should remain after delete");

        // Verify deleted store cannot be shown
        assertThrows(StoreException.class,
                () -> storeService.showStore(storeId, token),
                "Showing a deleted store should throw StoreException");
    }

    // =========================================================
    // 2) STORE WITH AISLES AND SHELVES
    // =========================================================

    @Test
    @Order(2)
    @DisplayName("Integration: Store with Aisles and Shelves")
    public void testStoreWithAislesAndShelves() throws StoreException {
        String token = "token";
        String storeId = "STORE-AISLE-1";

        storeService.provisionStore(storeId, "Aisle Store", "1 Aisle Way", token);

        // Provision aisle
        Aisle aisle = storeService.provisionAisle(
                storeId,
                "A1",
                "Grocery",
                "Dry goods aisle",
                AisleLocation.floor,
                token);
        assertNotNull(aisle);
        assertEquals("A1", aisle.getNumber());
        assertEquals("Grocery", aisle.getName());

        // Show aisle
        Aisle fetchedAisle = storeService.showAisle(storeId, "A1", token);
        assertNotNull(fetchedAisle);
        assertEquals("A1", fetchedAisle.getNumber());

        // Provision shelf
        Shelf shelf = storeService.provisionShelf(
                storeId,
                "A1",
                "S1",
                "Top Shelf",
                ShelfLevel.high,
                "Top shelf description",
                Temperature.ambient,
                token);
        assertNotNull(shelf);
        assertEquals("S1", shelf.getId());
        assertEquals(ShelfLevel.high, shelf.getLevel());
        assertEquals(Temperature.ambient, shelf.getTemperature());

        // Show shelf
        Shelf fetchedShelf = storeService.showShelf(storeId, "A1", "S1", token);
        assertNotNull(fetchedShelf);
        assertEquals("S1", fetchedShelf.getId());
        assertEquals(ShelfLevel.high, fetchedShelf.getLevel());
    }

    // =========================================================
    // 3) PRODUCT & INVENTORY WORKFLOW
    // =========================================================

    @Test
    @Order(3)
    @DisplayName("Integration: Product and Inventory workflow")
    public void testProductAndInventoryWorkflow() throws StoreException {
        String token = "token";
        String storeId = "STORE-PROD-1";

        // Store, aisle, shelf
        storeService.provisionStore(storeId, "Product Store", "11 Product Way", token);
        storeService.provisionAisle(storeId, "A1", "Grocery", "Dry goods", AisleLocation.floor, token);
        storeService.provisionShelf(storeId, "A1", "S1", "Shelf 1", ShelfLevel.medium,
                "Middle shelf", Temperature.ambient, token);

        // Product
        String productId = "PROD-1";
        Product product = storeService.provisionProduct(
                productId,
                "Milk",
                "1L Whole Milk",
                "1L",
                "dairy",
                1.0,
                Temperature.refrigerated,
                token);
        assertNotNull(product);
        assertEquals(productId, product.getId());
        assertEquals("Milk", product.getName());

        Product fetchedProduct = storeService.showProduct(productId, token);
        assertNotNull(fetchedProduct);
        assertEquals(productId, fetchedProduct.getId());

        // Inventory
        String inventoryId = "INV-1";
        Inventory inventory = storeService.provisionInventory(
                inventoryId,
                storeId,
                "A1",
                "S1",
                100,
                10,
                productId,
                InventoryType.standard,
                token);
        assertNotNull(inventory);
        assertEquals(inventoryId, inventory.getId());
        assertEquals(100, inventory.getCapacity());
        assertEquals(10, inventory.getCount());
        assertEquals(productId, inventory.getProductId());

        Inventory fetchedInventory = storeService.showInventory(inventoryId, token);
        assertNotNull(fetchedInventory);
        assertEquals(inventoryId, fetchedInventory.getId());

        Inventory updatedInventory = storeService.updateInventory(inventoryId, 25, token);
        assertEquals(25, updatedInventory.getCount());
    }

    // =========================================================
    // 4) CUSTOMER & BASKET WORKFLOW
    // =========================================================

    @Test
    @Order(4)
    @DisplayName("Integration: Customer and Basket workflow")
    public void testCustomerAndBasketWorkflow() throws StoreException {
        String token = "token";
        String storeId = "STORE-CUST-1";

        // Store + product
        storeService.provisionStore(storeId, "Customer Store", "22 Customer Way", token);
        String productId = "PROD-CUST-1";
        storeService.provisionProduct(productId, "Apple", "Red Apple", "1pc", "produce", 0.2, Temperature.ambient,
                token);

        // Customer
        String customerId = "CUST-1";
        Customer customer = storeService.provisionCustomer(
                customerId,
                "John",
                "Doe",
                CustomerType.registered,
                "john.doe@example.com",
                "22 Customer Way",
                token);
        assertNotNull(customer);
        assertEquals(customerId, customer.getId());
        assertEquals("John", customer.getFirstName());
        assertNull(customer.getBasket(), "Customer should not initially have a basket");

        // Basket
        String basketId = "BASK-1";
        Basket basket = storeService.provisionBasket(basketId, token);
        assertNotNull(basket);
        assertEquals(basketId, basket.getId());

        // Assign basket to customer
        storeService.assignCustomerBasket(customerId, basketId, token);
        Basket customerBasket = storeService.getCustomerBasket(customerId, token);
        assertNotNull(customerBasket);
        assertEquals(basketId, customerBasket.getId());
        assertNotNull(customerBasket.getCustomer());
        assertEquals(customerId, customerBasket.getCustomer().getId());

        // Basket initially empty
        assertTrue(customerBasket.getProductMap().isEmpty(), "Basket should start empty");

        // Add products
        storeService.addBasketProduct(basketId, productId, 2, token);
        Basket basketAfterAdd = storeService.showBasket(basketId, token);
        assertEquals(1, basketAfterAdd.getProductMap().size());
        assertTrue(basketAfterAdd.getProductMap().containsKey(productId));
        assertEquals(2, basketAfterAdd.getProductMap().get(productId));

        // Remove one
        storeService.removeBasketProduct(basketId, productId, 1, token);
        Basket basketAfterRemove = storeService.showBasket(basketId, token);
        assertEquals(1, basketAfterRemove.getProductMap().size());
        assertEquals(1, basketAfterRemove.getProductMap().get(productId));

        // Clear basket
        storeService.clearBasket(basketId, token);
        Basket basketAfterClear = storeService.showBasket(basketId, token);
        assertTrue(basketAfterClear.getProductMap().isEmpty(), "Basket should be empty after clear");
    }

    // =========================================================
    // 5) AUTHENTICATION WORKFLOW
    // =========================================================

    @Test
    @Order(5)
    @DisplayName("Integration: Authentication service full workflow")
    public void testAuthenticationWorkflow() {
        String email = "auth.user@example.com";
        String password = "secret";
        String name = "Auth User";

        // Register
        User registered = authenticationService.registerUser(email, password, name);
        assertNotNull(registered);
        assertEquals(email, registered.getEmail());
        assertEquals(name, registered.getName());

        // Get all users
        Collection<User> users = authenticationService.getAllUsers();
        assertEquals(1, users.size(), "There should be exactly one registered user");

        // Get by email
        User found = authenticationService.getUserByEmail(email);
        assertNotNull(found);
        assertEquals(email, found.getEmail());

        // Update
        String newPassword = "newSecret";
        String newName = "Updated User";
        User updated = authenticationService.updateUser(email, newPassword, newName);
        assertNotNull(updated);
        assertEquals(newName, updated.getName());

        // Delete
        boolean deleted = authenticationService.deleteUser(email);
        assertTrue(deleted, "Expected deleteUser to return true for existing user");
        assertNull(authenticationService.getUserByEmail(email),
                "User should not be found after deletion");
        assertTrue(authenticationService.getAllUsers().isEmpty(),
                "No users should remain after deletion");
    }

    // =========================================================
    // 6) DEVICE PROVISIONING & EVENTS
    // =========================================================

    @Test
    @Order(6)
    @DisplayName("Integration: Device provisioning and events")
    public void testDeviceProvisioningAndEvents() throws StoreException {
        String token = "token";
        String storeId = "STORE-DEV-1";

        storeService.provisionStore(storeId, "Device Store", "33 Device Way", token);
        // create an aisle for devices
        storeService.provisionAisle(storeId, "A1", "Devices", "Devices aisle", AisleLocation.floor, token);

        // Sensor device
        Device sensor = storeService.provisionDevice(
                "DEV-1",
                "Front Door Camera",
                SensorType.camera.name(),
                storeId,
                "A1",
                token);
        assertNotNull(sensor);
        assertEquals("DEV-1", sensor.getId());
        assertEquals(SensorType.camera.name(), sensor.getType());

        Device fetchedSensor = storeService.showDevice("DEV-1", token);
        assertNotNull(fetchedSensor);
        assertEquals("DEV-1", fetchedSensor.getId());

        // Event on sensor (should not throw)
        assertDoesNotThrow(() -> storeService.raiseEvent("DEV-1", "MOTION_DETECTED", token));

        // Appliance device
        Device appliance = storeService.provisionDevice(
                "DEV-2",
                "Store Robot",
                ApplianceType.robot.name(),
                storeId,
                "A1",
                token);
        assertNotNull(appliance);
        assertEquals("DEV-2", appliance.getId());
        assertEquals(ApplianceType.robot.name(), appliance.getType());

        // Command to appliance (should not throw)
        assertDoesNotThrow(() -> storeService.issueCommand("DEV-2", "START", token));
    }

    // =========================================================
    // 7) ERROR HANDLING ACROSS SERVICES
    // =========================================================

    @Test
    @Order(7)
    @DisplayName("Integration: Error handling across services")
    public void testErrorHandling() {
        String token = "token";

        // Unknown store - show
        StoreException showEx = assertThrows(StoreException.class,
                () -> storeService.showStore("UNKNOWN_STORE", token),
                "Expected StoreException for unknown store (show)");
        assertEquals("Show Store", showEx.getAction());
        assertEquals("Store Does Not Exist", showEx.getReason());

        // Unknown store - delete
        StoreException deleteEx = assertThrows(StoreException.class,
                () -> storeService.deleteStore("UNKNOWN_STORE"),
                "Expected StoreException for unknown store (delete)");
        assertEquals("Delete Store", deleteEx.getAction());
        assertEquals("Store Does Not Exist", deleteEx.getReason());

        // Unknown device - raise event
        StoreException eventEx = assertThrows(StoreException.class,
                () -> storeService.raiseEvent("UNKNOWN_DEVICE", "doorOpen", token),
                "Expected StoreException for unknown device (raiseEvent)");
        assertEquals("Raise Event", eventEx.getAction());
        assertEquals("Device Does Not Exist", eventEx.getReason());

        // Unknown device - issue command
        StoreException cmdEx = assertThrows(StoreException.class,
                () -> storeService.issueCommand("UNKNOWN_DEVICE", "RESET", token),
                "Expected StoreException for unknown device (issueCommand)");
        assertEquals("Issue Command", cmdEx.getAction());
        assertEquals("Device Does Not Exist", cmdEx.getReason());

        // Authentication: deleting non-existent user
        boolean deleted = authenticationService.deleteUser("missing@example.com");
        assertFalse(deleted, "Deleting an unknown user should return false");
        assertNull(authenticationService.getUserByEmail("missing@example.com"),
                "Unknown user lookup should return null");
    }
}
