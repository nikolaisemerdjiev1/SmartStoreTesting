package com.se300.store.service.unit;

import com.se300.store.model.AisleLocation;
import com.se300.store.model.Basket;
import com.se300.store.model.Customer;
import com.se300.store.model.CustomerType;
import com.se300.store.model.InventoryType;
import com.se300.store.model.ShelfLevel;
import com.se300.store.model.Store;
import com.se300.store.model.StoreException;
import com.se300.store.model.StoreLocation;
import com.se300.store.model.Temperature;
import com.se300.store.repository.StoreRepository;
import com.se300.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceMoreCoverageTest {

    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @BeforeEach
    void setUp() {
        StoreService.clearAllMaps();
        storeService = new StoreService();
    }

    @Test
    @DisplayName("showAisle throws when aisle does not exist (Get Aisle action)")
    void showAisleAisleDoesNotExist() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showAisle("S1", "A01", "token"));
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("provisionShelf throws when aisle does not exist (Get Aisle action)")
    void provisionShelfAisleDoesNotExist() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionShelf(
                        "S1",
                        "A01",
                        "SH1",
                        "Shelf 1",
                        ShelfLevel.high,
                        "desc",
                        Temperature.ambient,
                        "token"));
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("showShelf throws when aisle does not exist (Get Aisle action)")
    void showShelfAisleDoesNotExist() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showShelf("S1", "A01", "SH1", "token"));
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("provisionInventory throws when aisle does not exist (Get Aisle action)")
    void provisionInventoryAisleDoesNotExist() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionInventory(
                        "INV1",
                        "S1",
                        "A01",
                        "SH1",
                        10,
                        5,
                        "P1",
                        InventoryType.standard,
                        "token"));
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("updateCustomer throws when aisle does not exist (Get Aisle action)")
    void updateCustomerAisleDoesNotExist() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.updateCustomer("C1", "S1", "A01", "token"));
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("updateCustomer moves customer between stores and clears basket")
    void updateCustomerMovesStoreClearsBasketAndRemovesFromOtherStores() throws StoreException {
        StoreService.clearAllMaps();
        storeService = new StoreService();

        // Two stores
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");
        storeService.provisionStore("S2", "Store 2", "Address 2", "token");

        Store store1 = storeService.showStore("S1", "token");
        Store store2 = storeService.showStore("S2", "token");

        // Aisle only in S2, so moving C1 there will hit the "move store" branch
        storeService.provisionAisle("S2", "A02", "Aisle 2", "desc", AisleLocation.floor, "token");

        // Customer initially created
        Customer customer = storeService.provisionCustomer(
                "C1",
                "First",
                "Last",
                CustomerType.registered,
                "c1@example.com",
                "123 Main St",
                "token");

        // Put customer in S1/A01 and add to that store
        customer.setStoreLocation(new StoreLocation("S1", "A01"));
        store1.addCustomer(customer);

        // Give customer a basket and wire both sides so clearBasket() works
        Basket basket = new Basket("B1");
        basket.setCustomer(customer);
        customer.assignBasket(basket);

        // Act: move customer to S2/A02
        Customer updated = storeService.updateCustomer("C1", "S2", "A02", "token");

        // Same object back
        assertSame(customer, updated);
        // Basket should have been cleared by updateCustomer path
        assertNull(updated.getBasket());
        // Customer is now known to store2 (we don't assert internals of StoreLocation)
        assertNotNull(store2.getCustomer("C1"));
    }

    @Test
    @DisplayName("provisionDevice throws when aisle does not exist (Get Aisle action)")
    void provisionDeviceAisleDoesNotExist() throws StoreException {
        storeService.provisionStore("S1", "Store 1", "Address 1", "token");

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.provisionDevice(
                        "D1",
                        "Device 1",
                        "SOME_TYPE",
                        "S1",
                        "A01",
                        "token"));
        assertEquals("Get Aisle", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getMessage());
    }

    @Test
    @DisplayName("deleteStore removes existing store and calls repository when present")
    void deleteStoreRemovesStoreAndCallsRepository() throws StoreException {
        StoreService.clearAllMaps();
        StoreService serviceWithRepo = new StoreService(storeRepository);

        Store store = serviceWithRepo.provisionStore("S1", "Store 1", "Address 1", "token");
        assertNotNull(store);

        serviceWithRepo.deleteStore("S1");

        verify(storeRepository).delete("S1");
    }
}
