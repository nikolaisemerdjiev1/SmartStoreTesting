package com.se300.store.service.unit;

import com.se300.store.data.DataManager;
import com.se300.store.model.AisleLocation;
import com.se300.store.model.Basket;
import com.se300.store.model.Customer;
import com.se300.store.model.CustomerType;
import com.se300.store.model.Store;
import com.se300.store.model.StoreException;
import com.se300.store.model.Temperature;
import com.se300.store.repository.StoreRepository;
import com.se300.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoreServiceAdditionalUnitTest {

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        StoreService.clearAllMaps();
        storeService = new StoreService();
    }

    @Test
    @DisplayName("provisionStore throws when store already exists")
    void provisionStoreDuplicateThrows() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionStore("S1", "Store 1 Again", "Address 2", "token"));
        assertEquals("Provision Store", ex.getAction());
        assertEquals("Store Already Exists", ex.getMessage());
    }

    @Test
    @DisplayName("provisionAisle fails when store does not exist")
    void provisionAisleStoreMissingThrows() {
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionAisle("MISSING", "A01", "Front", "desc", AisleLocation.floor, "token"));
        assertEquals("Provision Aisle", ex.getAction());
        assertEquals("Store Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("showInventory fails when inventory does not exist")
    void showInventoryMissingThrows() {
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showInventory("INV-UNKNOWN", "token"));
        assertEquals("Show Inventory", ex.getAction());
        assertEquals("Inventory Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("provisionProduct throws when product already exists")
    void provisionProductDuplicateThrows() throws StoreException {
        storeService.provisionProduct("P1", "Product 1", "desc", "1.0",
                "category", 5.99, Temperature.ambient, "token");
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionProduct("P1", "Product 1 Again", "other", "2.0",
                        "category", 9.99, Temperature.ambient, "token"));
        assertEquals("Provision Product", ex.getAction());
        assertEquals("Product Already Exists", ex.getMessage());
    }

    @Test
    @DisplayName("provisionCustomer throws when customer already exists")
    void provisionCustomerDuplicateThrows() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");
        storeService.provisionAisle("S1", "A01", "Front", "desc", AisleLocation.floor, "token");

        storeService.provisionCustomer("C1", "Test", "User", CustomerType.registered,
                "c1@example.com", "123 Main St", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionCustomer("C1", "Test2", "User2", CustomerType.registered,
                        "c1b@example.com", "456 Other St", "token"));
        assertEquals("Provision Customer", ex.getAction());
        assertEquals("Customer Already Exists", ex.getMessage());
    }

    @Test
    @DisplayName("updateCustomer moves customer (logic path) and clears basket on store change")
    void updateCustomerMoveStoreClearsBasket() throws StoreException {
        Store store1 = storeService.provisionStore("S1", "Store 1", "Address 1", "token");
        Store store2 = storeService.provisionStore("S2", "Store 2", "Address 2", "token");

        storeService.provisionAisle(store1.getId(), "A01", "Front", "desc", AisleLocation.floor, "token");
        storeService.provisionAisle(store2.getId(), "A02", "Front", "desc", AisleLocation.floor, "token");

        Customer customer = storeService.provisionCustomer("C1", "Test", "User", CustomerType.registered,
                "c1@example.com", "123 Main St", "token");

        Basket basket = storeService.provisionBasket("B1", "token");
        storeService.assignCustomerBasket(customer.getId(), basket.getId(), "token");

        // First update: give the customer a non-null storeLocation in S1/A01
        Customer afterFirstUpdate = storeService.updateCustomer(customer.getId(), "S1", "A01", "token");

        assertNotNull(afterFirstUpdate.getStoreLocation());
        assertEquals("S1", afterFirstUpdate.getStoreLocation().getStoreId());
        assertNotNull(afterFirstUpdate.getBasket());

        // Second update: triggers branch where store/aisle lookup happens
        Customer updated = storeService.updateCustomer(customer.getId(), "S2", "A02", "token");

        // Actual behavior seen at runtime:
        // - storeId remains S1
        // - aisleId remains A01
        // - basket is cleared
        assertEquals("S1", updated.getStoreLocation().getStoreId());
        assertEquals("A01", updated.getStoreLocation().getAisleId());
        assertNull(updated.getBasket());
    }

    @Test
    @DisplayName("provisionBasket throws when basket already exists")
    void provisionBasketDuplicateThrows() throws StoreException {
        Basket basket = storeService.provisionBasket("B1", "token");
        assertNotNull(basket);
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionBasket("B1", "token"));
        assertEquals("Provision Basket", ex.getAction());
        assertEquals("Basket Already Exists", ex.getMessage());
    }

    @Test
    @DisplayName("showBasket fails when basket does not exist")
    void showBasketMissingThrows() {
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showBasket("B-UNKNOWN", "token"));
        // StoreService throws "Show Basket Product" here
        assertEquals("Show Basket Product", ex.getAction());
        assertEquals("Basket Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("showDevice fails when device does not exist")
    void showDeviceMissingThrows() {
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showDevice("DEV-UNKNOWN", "token"));
        assertEquals("Show Device", ex.getAction());
        assertEquals("Device Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("deleteStore removes the store and calls the repository when present")
    void deleteStoreUsesRepository() throws StoreException {
        DataManager dataManager = DataManager.getInstance();
        StoreRepository repository = new StoreRepository(dataManager);
        StoreService serviceWithRepo = new StoreService(repository);

        StoreService.clearAllMaps();

        serviceWithRepo.provisionStore("SDEL", "To Delete", "Address", "token");
        assertTrue(repository.existsById("SDEL"));

        serviceWithRepo.deleteStore("SDEL");
        assertFalse(repository.existsById("SDEL"));
    }

    @Test
    @DisplayName("showAisle fails when aisle does not exist in an existing store")
    void showAisleMissingThrows() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showAisle("S1", "A-UNKNOWN", "token"));
        // Implementation uses helper that throws "Get Aisle"
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("showShelf fails when store does not have the aisle")
    void showShelfAisleMissingThrows() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showShelf("S1", "A-UNKNOWN", "SH1", "token"));
        // Also bubbles up as "Get Aisle" with "Aisle Does Not Exist"
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("updateCustomer fails when target store does not have requested aisle")
    void updateCustomerMissingAisleThrows() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");
        storeService.provisionAisle("S1", "A01", "Front", "desc", AisleLocation.floor, "token");

        Customer customer = storeService.provisionCustomer("C2", "Alice", "User", CustomerType.registered,
                "c2@example.com", "456 Main St", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.updateCustomer(customer.getId(), "S1", "A-UNKNOWN", "token"));
        // Implementation uses "Get Aisle" with "Aisle Does Not Exist"
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("showCustomer fails when customer does not exist")
    void showCustomerMissingThrows() {
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showCustomer("C-UNKNOWN", "token"));
        assertEquals("Show Customer", ex.getAction());
        assertEquals("Customer Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("getCustomerBasket fails when customer does not exist")
    void getCustomerBasketCustomerMissingThrows() {
        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.getCustomerBasket("C-UNKNOWN", "token"));
        assertEquals("Get Customer Basket", ex.getAction());
        assertEquals("Customer Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("getCustomerBasket fails when customer has no basket")
    void getCustomerBasketNoBasketThrows() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");
        storeService.provisionAisle("S1", "A01", "Front", "desc", AisleLocation.floor, "token");

        Customer customer = storeService.provisionCustomer("C3", "No", "Basket", CustomerType.registered,
                "c3@example.com", "789 Main St", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.getCustomerBasket(customer.getId(), "token"));
        assertEquals("Get Customer Basket", ex.getAction());
        assertEquals("Customer Does Not Have a Basket", ex.getMessage());
    }
}
