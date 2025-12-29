package com.se300.store.service.unit;

import com.se300.store.model.*;
import com.se300.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests focused on exercising error branches and edge cases
 * in StoreService to improve code coverage.
 */
public class StoreServiceErrorUnitTest {

    private StoreService storeService;
    private final String token = "token";

    @BeforeEach
    public void setUp() {
        StoreService.clearAllMaps();
        storeService = new StoreService();
    }

    // ---------------------------------------------------------
    // Basket-related error paths
    // ---------------------------------------------------------

    @Test
    @DisplayName("AddBasketProduct: validates basket, product and assignment")
    public void testAddBasketProductErrorPaths() throws StoreException {
        // 1) Basket does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.addBasketProduct("B1", "P1", 1, token));
        assertEquals("Add Basket Product", ex1.getAction());
        assertEquals("Basket Does Not Exist", ex1.getReason());

        // 2) Basket exists but product does not
        storeService.provisionBasket("B1", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.addBasketProduct("B1", "P1", 1, token));
        assertEquals("Add Basket Product", ex2.getAction());
        assertEquals("Product Does Not Exist", ex2.getReason());

        // 3) Basket and product exist but basket is not assigned to a customer
        storeService.provisionProduct("P1", "Apple", "Red", "1pc", "produce",
                0.5, Temperature.ambient, token);

        StoreException ex3 = assertThrows(StoreException.class,
                () -> storeService.addBasketProduct("B1", "P1", 1, token));
        assertEquals("Add Basket Product", ex3.getAction());
        assertEquals("Basket Has Not Being Assigned", ex3.getReason());
    }

    @Test
    @DisplayName("RemoveBasketProduct: validates basket, product and assignment")
    public void testRemoveBasketProductErrorPaths() throws StoreException {
        // 1) Basket does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.removeBasketProduct("B1", "P1", 1, token));
        assertEquals("Remove Basket Product", ex1.getAction());
        assertEquals("Basket Does Not Exist", ex1.getReason());

        // 2) Basket exists but product does not
        storeService.provisionBasket("B1", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.removeBasketProduct("B1", "P1", 1, token));
        assertEquals("Remove Basket Product", ex2.getAction());
        assertEquals("Product Does Not Exist", ex2.getReason());

        // 3) Basket and product exist but basket is not assigned to a customer
        storeService.provisionProduct("P1", "Apple", "Red", "1pc", "produce",
                0.5, Temperature.ambient, token);

        StoreException ex3 = assertThrows(StoreException.class,
                () -> storeService.removeBasketProduct("B1", "P1", 1, token));
        assertEquals("Remove Basket Product", ex3.getAction());
        assertEquals("Basket Has Not Being Assigned", ex3.getReason());
    }

    @Test
    @DisplayName("AssignCustomerBasket: validates missing customer and basket")
    public void testAssignCustomerBasketValidation() throws StoreException {
        // 1) Customer does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.assignCustomerBasket("C1", "B1", token));
        assertEquals("Assign Customer Basket", ex1.getAction());
        assertEquals("Customer Does Not Exist", ex1.getReason());

        // 2) Customer exists but basket does not
        storeService.provisionCustomer("C1", "John", "Doe",
                CustomerType.registered, "john@example.com", "123 Street", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.assignCustomerBasket("C1", "B1", token));
        assertEquals("Assign Customer Basket", ex2.getAction());
        assertEquals("Basket Does Not Exist", ex2.getReason());
    }

    @Test
    @DisplayName("AssignCustomerBasket: associates basket with store when customer has a location")
    public void testAssignCustomerBasketAssociatesStore() throws StoreException {
        String storeId = "STORE-1";
        String aisleNumber = "A1";
        String customerId = "C1";
        String basketId = "B1";

        storeService.provisionStore(storeId, "Main Store", "1 Store Way", token);
        storeService.provisionAisle(storeId, aisleNumber,
                "Grocery", "Dry goods", AisleLocation.floor, token);

        storeService.provisionCustomer(customerId, "John", "Doe",
                CustomerType.registered, "john@example.com", "123 Street", token);
        storeService.updateCustomer(customerId, storeId, aisleNumber, token);

        storeService.provisionBasket(basketId, token);

        Basket basket = storeService.assignCustomerBasket(customerId, basketId, token);

        assertNotNull(basket);
        assertNotNull(basket.getCustomer());
        assertEquals(customerId, basket.getCustomer().getId());
        assertNotNull(basket.getStore());
        assertEquals(storeId, basket.getStore().getId());
    }

    @Test
    @DisplayName("GetCustomerBasket: validates customer existence and basket assignment")
    public void testGetCustomerBasketErrorPaths() throws StoreException {
        // 1) Customer does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.getCustomerBasket("C1", token));
        assertEquals("Get Customer Basket", ex1.getAction());
        assertEquals("Customer Does Not Exist", ex1.getReason());

        // 2) Customer exists but has no basket
        storeService.provisionCustomer("C1", "John", "Doe",
                CustomerType.registered, "john@example.com", "123 Street", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.getCustomerBasket("C1", token));
        assertEquals("Get Customer Basket", ex2.getAction());
        assertEquals("Customer Does Not Have a Basket", ex2.getReason());
    }

    @Test
    @DisplayName("ClearBasket: validates basket existence and assignment")
    public void testClearBasketErrorPaths() throws StoreException {
        // 1) Basket does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.clearBasket("B1", token));
        assertEquals("Clear Basket", ex1.getAction());
        assertEquals("Basket Does Not Exist", ex1.getReason());

        // 2) Basket exists but is not assigned to a customer
        storeService.provisionBasket("B1", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.clearBasket("B1", token));
        assertEquals("Clear Basket", ex2.getAction());
        assertEquals("Basket Has Not Being Assigned", ex2.getReason());
    }

    // ---------------------------------------------------------
    // Customer and store update paths
    // ---------------------------------------------------------

    @Test
    @DisplayName("UpdateCustomer: validates store, aisle and customer existence")
    public void testUpdateCustomerValidation() throws StoreException {
        String storeId = "STORE-1";
        String aisleNumber = "A1";
        String customerId = "C1";

        // 1) Store does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.updateCustomer(customerId, storeId, aisleNumber, token));
        assertEquals("Store Does Not Exist", ex1.getReason());

        // 2) Store exists but aisle does not
        storeService.provisionStore(storeId, "Main Store", "1 Store Way", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.updateCustomer(customerId, storeId, aisleNumber, token));
        assertEquals("Aisle Does Not Exist", ex2.getReason());

        // 3) Store and aisle exist but customer does not
        storeService.provisionAisle(storeId, aisleNumber,
                "Grocery", "Dry goods", AisleLocation.floor, token);

        StoreException ex3 = assertThrows(StoreException.class,
                () -> storeService.updateCustomer(customerId, storeId, aisleNumber, token));
        assertEquals("Customer Does Not Exist", ex3.getReason());
    }

    @Test
    @DisplayName("UpdateStore: validates existence and supports partial updates")
    public void testUpdateStoreValidationAndPartialUpdate() throws StoreException {
        String storeId = "STORE-1";

        // 1) Store does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.updateStore(storeId, "New Description", "New Address"));
        assertEquals("Update Store", ex1.getAction());
        assertEquals("Store Does Not Exist", ex1.getReason());

        // 2) Provision store
        Store store = storeService.provisionStore(storeId, "Old Description", "Old Address", token);

        // 3) Update only description
        Store updated1 = storeService.updateStore(storeId, "New Description", null);
        assertSame(store, updated1);
        assertEquals("New Description", updated1.getDescription());
        assertEquals("Old Address", updated1.getAddress());

        // 4) Update only address
        Store updated2 = storeService.updateStore(storeId, null, "New Address");
        assertSame(store, updated2);
        assertEquals("New Description", updated2.getDescription());
        assertEquals("New Address", updated2.getAddress());
    }

    // ---------------------------------------------------------
    // Provision / show shelf, inventory and device error paths
    // ---------------------------------------------------------

    @Test
    @DisplayName("ProvisionShelf: validates store, aisle and existing shelf")
    public void testProvisionShelfErrorPaths() throws StoreException {
        String storeId = "STORE-1";
        String aisleNumber = "A1";
        String shelfId = "SHELF-1";

        // 1) Store does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.provisionShelf(storeId, aisleNumber, shelfId,
                        "Top Shelf", ShelfLevel.high, "Desc", Temperature.ambient, token));
        assertEquals("Store Does Not Exist", ex1.getReason());

        // 2) Store exists but aisle does not
        storeService.provisionStore(storeId, "Main Store", "1 Store Way", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.provisionShelf(storeId, aisleNumber, shelfId,
                        "Top Shelf", ShelfLevel.high, "Desc", Temperature.ambient, token));
        assertEquals("Aisle Does Not Exist", ex2.getReason());

        // 3) Aisle exists, provision shelf once
        storeService.provisionAisle(storeId, aisleNumber,
                "Grocery", "Dry goods", AisleLocation.floor, token);
        Shelf shelf = storeService.provisionShelf(storeId, aisleNumber, shelfId,
                "Top Shelf", ShelfLevel.high, "Desc", Temperature.ambient, token);
        assertNotNull(shelf);

        // 4) Provisioning the same shelf again should fail
        StoreException ex3 = assertThrows(StoreException.class,
                () -> storeService.provisionShelf(storeId, aisleNumber, shelfId,
                        "Top Shelf", ShelfLevel.high, "Desc", Temperature.ambient, token));
        assertEquals("Shelf Already Exists", ex3.getReason());
    }

    @Test
    @DisplayName("ShowShelf: validates store, aisle and shelf existence")
    public void testShowShelfErrorPaths() throws StoreException {
        String storeId = "STORE-1";
        String aisleNumber = "A1";
        String shelfId = "SHELF-1";

        // 1) Store does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.showShelf(storeId, aisleNumber, shelfId, token));
        assertEquals("Store Does Not Exist", ex1.getReason());

        // 2) Store exists but aisle does not
        storeService.provisionStore(storeId, "Main Store", "1 Store Way", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.showShelf(storeId, aisleNumber, shelfId, token));
        assertEquals("Aisle Does Not Exist", ex2.getReason());

        // 3) Aisle exists but shelf does not
        storeService.provisionAisle(storeId, aisleNumber,
                "Grocery", "Dry goods", AisleLocation.floor, token);

        StoreException ex3 = assertThrows(StoreException.class,
                () -> storeService.showShelf(storeId, aisleNumber, shelfId, token));
        assertEquals("Shelf Does Not Exist", ex3.getReason());

        // 4) Provision shelf and then successfully show it
        storeService.provisionShelf(storeId, aisleNumber, shelfId,
                "Top Shelf", ShelfLevel.high, "Desc", Temperature.ambient, token);
        Shelf shelf = storeService.showShelf(storeId, aisleNumber, shelfId, token);
        assertNotNull(shelf);
        assertEquals(shelfId, shelf.getId());
    }

    @Test
    @DisplayName("ShowAisle: validates store and aisle existence")
    public void testShowAisleErrorPaths() throws StoreException {
        String storeId = "STORE-1";
        String aisleNumber = "A1";

        // 1) Store does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.showAisle(storeId, aisleNumber, token));
        assertEquals("Store Does Not Exist", ex1.getReason());

        // 2) Store exists but aisle does not
        storeService.provisionStore(storeId, "Main Store", "1 Store Way", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.showAisle(storeId, aisleNumber, token));
        assertEquals("Aisle Does Not Exist", ex2.getReason());

        // 3) Aisle exists and can be shown
        storeService.provisionAisle(storeId, aisleNumber,
                "Grocery", "Dry goods", AisleLocation.floor, token);
        Aisle aisle = storeService.showAisle(storeId, aisleNumber, token);
        assertNotNull(aisle);
        assertEquals(aisleNumber, aisle.getNumber());
    }

    @Test
    @DisplayName("ProvisionInventory: validates store, aisle, shelf and product existence")
    public void testProvisionInventoryErrorPaths() throws StoreException {
        String storeId = "STORE-1";
        String aisleNumber = "A1";
        String shelfId = "SHELF-1";
        String inventoryId = "INV-1";
        String productId = "P1";

        // 1) Store does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.provisionInventory(inventoryId, storeId, aisleNumber, shelfId,
                        10, 5, productId, InventoryType.standard, token));
        assertEquals("Store Does Not Exist", ex1.getReason());

        // 2) Store exists but aisle does not
        storeService.provisionStore(storeId, "Main Store", "1 Store Way", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.provisionInventory(inventoryId, storeId, aisleNumber, shelfId,
                        10, 5, productId, InventoryType.standard, token));
        assertEquals("Aisle Does Not Exist", ex2.getReason());

        // 3) Aisle exists but shelf does not
        storeService.provisionAisle(storeId, aisleNumber,
                "Grocery", "Dry goods", AisleLocation.floor, token);

        StoreException ex3 = assertThrows(StoreException.class,
                () -> storeService.provisionInventory(inventoryId, storeId, aisleNumber, shelfId,
                        10, 5, productId, InventoryType.standard, token));
        assertEquals("Shelf Does Not Exist", ex3.getReason());

        // 4) Shelf exists but product does not
        storeService.provisionShelf(storeId, aisleNumber, shelfId,
                "Top Shelf", ShelfLevel.high, "Desc", Temperature.ambient, token);

        StoreException ex4 = assertThrows(StoreException.class,
                () -> storeService.provisionInventory(inventoryId, storeId, aisleNumber, shelfId,
                        10, 5, productId, InventoryType.standard, token));
        assertEquals("Product Does Not Exist", ex4.getReason());

        // 5) Product exists and inventory can be provisioned
        storeService.provisionProduct(productId, "Apple", "Red", "1pc", "produce",
                0.5, Temperature.ambient, token);

        Inventory inventory = storeService.provisionInventory(inventoryId, storeId, aisleNumber, shelfId,
                10, 5, productId, InventoryType.standard, token);
        assertNotNull(inventory);
        assertEquals(inventoryId, inventory.getId());
    }

    @Test
    @DisplayName("ProvisionDevice: validates store, aisle and duplicate device")
    public void testProvisionDeviceErrorPaths() throws StoreException {
        String storeId = "STORE-1";
        String aisleNumber = "A1";
        String deviceId = "DEV-1";

        // 1) Store does not exist
        StoreException ex1 = assertThrows(StoreException.class,
                () -> storeService.provisionDevice(deviceId, "Camera",
                        "microphone", storeId, aisleNumber, token));
        assertEquals("Store Does Not Exist", ex1.getReason());

        // 2) Store exists but aisle does not
        storeService.provisionStore(storeId, "Main Store", "1 Store Way", token);

        StoreException ex2 = assertThrows(StoreException.class,
                () -> storeService.provisionDevice(deviceId, "Camera",
                        "microphone", storeId, aisleNumber, token));
        assertEquals("Aisle Does Not Exist", ex2.getReason());

        // 3) Aisle exists, first provision succeeds
        storeService.provisionAisle(storeId, aisleNumber,
                "Grocery", "Dry goods", AisleLocation.floor, token);

        Device device = storeService.provisionDevice(deviceId, "Camera",
                "microphone", storeId, aisleNumber, token);
        assertNotNull(device);
        assertEquals(deviceId, device.getId());

        // 4) Provisioning the same device again should fail
        StoreException ex3 = assertThrows(StoreException.class,
                () -> storeService.provisionDevice(deviceId, "Camera",
                        "microphone", storeId, aisleNumber, token));
        assertEquals("Device Already Exists", ex3.getReason());
    }
}
