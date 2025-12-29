package com.se300.store.model.unit;

import com.se300.store.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Basket add/remove unit tests")
public class BasketAddRemoveUnitTest {

    private Customer newRegisteredCustomer(String id) {
        return new Customer(
                id,
                "Test",
                "User",
                CustomerType.registered,
                id.toLowerCase() + "@example.com",
                "123 Main St");
    }

    private Basket newBasketWithCustomer(Customer customer) {
        Basket basket = new Basket("B001");
        basket.setCustomer(customer);
        return basket;
    }

    private Store newStore(String id) {
        return new Store(id, "999 Test Rd", "Test Store");
    }

    // at class level keep a single Store for the tests
    private final Store store = new Store("S1", "123 Main St", "Test Store");

    private void attachBasketToStoreLocation(Basket basket, Customer customer, Store store, String aisleNumber) {
        basket.setStore(store);
        StoreLocation location = new StoreLocation(store.getId(), aisleNumber);
        customer.setStoreLocation(location);
    }

    private Inventory addInventory(Store targetStore,
            String aisleNumber,
            String shelfId,
            String productId,
            int capacity,
            int count) throws StoreException {

        // Reuse or create aisle
        Aisle aisle = targetStore.getAisle(aisleNumber);
        if (aisle == null) {
            aisle = targetStore.addAisle(aisleNumber, "Aisle " + aisleNumber, "Test Aisle " + aisleNumber,
                    AisleLocation.floor);
        }

        // Reuse or create shelf
        Shelf shelf = aisle.getShelf(shelfId);
        if (shelf == null) {
            shelf = aisle.addShelf(shelfId, "Shelf " + shelfId, ShelfLevel.high, "Test shelf " + shelfId,
                    Temperature.ambient);
        }

        String inventoryId = "INV-" + productId + "-" + shelfId;
        Inventory inv = shelf.addInventory(inventoryId, targetStore.getId(), aisleNumber, shelfId, capacity, count,
                productId, InventoryType.standard);
        targetStore.addInventory(inv);
        return inv;
    }

    @Test
    @DisplayName("Basket setId updates id")
    public void testSetIdUpdatesBasketId() {
        Basket basket = new Basket("B001");
        assertEquals("B001", basket.getId());

        basket.setId("B002");
        assertEquals("B002", basket.getId());
    }

    @Test
    @DisplayName("addProduct throws for guest customers")
    public void testAddProductGuestNotAllowed() {
        Customer guest = new Customer(
                "C-guest",
                "Guest",
                "User",
                CustomerType.guest,
                "guest@example.com",
                "No Address");
        Basket basket = new Basket("B001");
        basket.setCustomer(guest);

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.addProduct("P1", 1));

        assertEquals("Add Product", ex.getAction());
        assertEquals("Guests Are Not Allowed to Shop", ex.getReason());
    }

    @Test
    @DisplayName("addProduct without store just merges counts in basket")
    public void testAddProductWithoutStoreMergesQuantities() throws StoreException {
        Customer customer = newRegisteredCustomer("C1");
        Basket basket = newBasketWithCustomer(customer);
        // store is null, and storeLocation is null

        basket.addProduct("P1", 2);
        basket.addProduct("P1", 3);

        Map<String, Integer> map = basket.getProductMap();
        assertEquals(5, map.get("P1"));
    }

    @Test
    @DisplayName("addProduct throws when customer is not near product")
    public void testAddProductCustomerNotNearProductThrows() throws StoreException {
        Customer customer = newRegisteredCustomer("C2");
        Basket basket = newBasketWithCustomer(customer);

        Store store = newStore("S1");
        // Put some inventory for a different product so list is non-null but empty
        // after filter
        {
            Aisle aisleA01_1;
            try {
                aisleA01_1 = store.getAisle("A01");
            } catch (StoreException e) {
                aisleA01_1 = store.addAisle("A01", "Aisle A01", "Test aisle A01", AisleLocation.floor);
            }
            Shelf shelfSH1_1 = aisleA01_1.getShelf("SH1");
            if (shelfSH1_1 == null) {
                shelfSH1_1 = aisleA01_1.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test shelf SH1",
                        Temperature.ambient);
            }
            Inventory inv_OTHER_1 = shelfSH1_1.addInventory("INV-OTHER-SH1", store.getId(), "A01", "SH1", 10, 5,
                    "OTHER", InventoryType.standard);
            store.addInventory(inv_OTHER_1);
        }

        attachBasketToStoreLocation(basket, customer, store, "A01");

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.addProduct("P-TARGET", 1));

        assertEquals("Add Product", ex.getAction());
        assertEquals("Customer Is Not Near Product", ex.getReason());
    }

    @Test
    @DisplayName("addProduct throws when multiple inventories match product in aisle")
    public void testAddProductMultipleInventoryMatchesThrows() throws StoreException {
        Customer customer = newRegisteredCustomer("C3");
        Basket basket = newBasketWithCustomer(customer);

        Store store = newStore("S2");

        // Create ONE aisle, TWO shelves with same product in that aisle
        Aisle aisle = store.addAisle("A01", "Aisle A01", "Test Aisle", AisleLocation.floor);
        Shelf shelf1 = aisle.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test Shelf 1", Temperature.ambient);
        Inventory inv1 = shelf1.addInventory(
                "INV-P-MULTI-SH1", store.getId(), "A01", "SH1",
                10, 5, "P-MULTI", InventoryType.standard);
        store.addInventory(inv1);

        Shelf shelf2 = aisle.addShelf("SH2", "Shelf SH2", ShelfLevel.low, "Test Shelf 2", Temperature.ambient);
        Inventory inv2 = shelf2.addInventory(
                "INV-P-MULTI-SH2", store.getId(), "A01", "SH2",
                10, 5, "P-MULTI", InventoryType.standard);
        store.addInventory(inv2);

        attachBasketToStoreLocation(basket, customer, store, "A01");

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.addProduct("P-MULTI", 1));

        assertEquals("Add Product", ex.getAction());
        assertEquals("There Are Several Products In the Aisle", ex.getReason());
    }

    @Test
    @DisplayName("addProduct throws when not enough inventory on shelf")
    public void testAddProductInsufficientInventoryThrows() throws StoreException {
        Customer customer = newRegisteredCustomer("C4");
        Basket basket = newBasketWithCustomer(customer);

        Store store = newStore("S3");
        {
            Aisle aisleA01_2;
            try {
                aisleA01_2 = store.getAisle("A01");
            } catch (StoreException e) {
                aisleA01_2 = store.addAisle("A01", "Aisle A01", "Test aisle A01", AisleLocation.floor);
            }
            Shelf shelfSH1_2 = aisleA01_2.getShelf("SH1");
            if (shelfSH1_2 == null) {
                shelfSH1_2 = aisleA01_2.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test shelf SH1",
                        Temperature.ambient);
            }
            Inventory inv_P_LOW = shelfSH1_2.addInventory("INV-P-LOW-SH1", store.getId(), "A01", "SH1", 10, 2, "P-LOW",
                    InventoryType.standard);
            store.addInventory(inv_P_LOW);
        }

        attachBasketToStoreLocation(basket, customer, store, "A01");

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.addProduct("P-LOW", 3));

        assertEquals("Add Product", ex.getAction());
        assertEquals("There Is Not Enough Inventory on the Shelf", ex.getReason());
    }

    @Test
    @DisplayName("addProduct success updates basket and decrements shelf inventory")
    public void testAddProductSuccessUpdatesInventoryAndBasket() throws StoreException {
        Customer customer = newRegisteredCustomer("C5");
        Basket basket = newBasketWithCustomer(customer);

        Store store = newStore("S4");
        Inventory inventory;
        {
            Aisle aisleA01_3;
            try {
                aisleA01_3 = store.getAisle("A01");
            } catch (StoreException e) {
                aisleA01_3 = store.addAisle("A01", "Aisle A01", "Test aisle A01", AisleLocation.floor);
            }
            Shelf shelfSH1_3 = aisleA01_3.getShelf("SH1");
            if (shelfSH1_3 == null) {
                shelfSH1_3 = aisleA01_3.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test shelf SH1",
                        Temperature.ambient);
            }
            inventory = shelfSH1_3.addInventory("INV-P-OK-SH1", store.getId(), "A01", "SH1", 10, 8, "P-OK",
                    InventoryType.standard);
            store.addInventory(inventory);
        }

        attachBasketToStoreLocation(basket, customer, store, "A01");

        basket.addProduct("P-OK", 3);

        Map<String, Integer> map = basket.getProductMap();
        assertEquals(3, map.get("P-OK"));
        assertEquals(5, inventory.getCount());
    }

    @Test
    @DisplayName("removeProduct throws when product not in basket")
    public void testRemoveProductMissingProductThrows() {
        Customer customer = newRegisteredCustomer("C6");
        Basket basket = newBasketWithCustomer(customer);

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.removeProduct("P1", 1));

        assertEquals("Remove Product", ex.getAction());
        assertEquals("Product Does Not Exist", ex.getReason());
    }

    @Test
    @DisplayName("removeProduct throws when removing more than exists in basket")
    public void testRemoveProductMoreThanExistsThrows() throws StoreException {
        Customer customer = newRegisteredCustomer("C7");
        Basket basket = newBasketWithCustomer(customer);

        // No store, so basket just tracks counts
        basket.addProduct("P1", 3);

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.removeProduct("P1", 5));

        assertEquals("Remove Product", ex.getAction());
        assertEquals("Trying To Remove More Quantity Than Exists", ex.getReason());
    }

    @Test
    @DisplayName("removeProduct without store updates basket and removes entry at zero")
    public void testRemoveProductWithoutStoreUpdatesAndRemovesWhenZero() throws StoreException {
        Customer customer = newRegisteredCustomer("C8");
        Basket basket = newBasketWithCustomer(customer);

        basket.addProduct("P1", 5);

        basket.removeProduct("P1", 2);
        assertEquals(3, basket.getProductMap().get("P1"));

        basket.removeProduct("P1", 3);
        assertFalse(basket.getProductMap().containsKey("P1"));
    }

    @Test
    @DisplayName("removeProduct throws when customer is not near product")
    public void testRemoveProductCustomerNotNearProductThrows() throws StoreException {
        Customer customer = newRegisteredCustomer("C9");
        Basket basket = newBasketWithCustomer(customer);

        // Basket currently holds 2 units
        basket.addProduct("P-NOT-NEAR", 2);

        Store store = newStore("S5");
        // Different product in aisle so filter will be empty
        {
            Aisle aisleA01_4;
            try {
                aisleA01_4 = store.getAisle("A01");
            } catch (StoreException e) {
                aisleA01_4 = store.addAisle("A01", "Aisle A01", "Test aisle A01", AisleLocation.floor);
            }
            Shelf shelfSH1_4 = aisleA01_4.getShelf("SH1");
            if (shelfSH1_4 == null) {
                shelfSH1_4 = aisleA01_4.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test shelf SH1",
                        Temperature.ambient);
            }
            Inventory inv_OTHER_2 = shelfSH1_4.addInventory("INV-OTHER-SH1", store.getId(), "A01", "SH1", 10, 5,
                    "OTHER", InventoryType.standard);
            store.addInventory(inv_OTHER_2);
        }
        attachBasketToStoreLocation(basket, customer, store, "A01");

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.removeProduct("P-NOT-NEAR", 1));

        assertEquals("Remove Product", ex.getAction());
        assertEquals("Customer Is Not Near Product", ex.getReason());
    }

    @Test
    @DisplayName("removeProduct throws when multiple inventories match product in aisle")
    public void testRemoveProductMultipleInventoryMatchesThrows() throws StoreException {
        Customer customer = newRegisteredCustomer("C10");
        Basket basket = newBasketWithCustomer(customer);

        basket.addProduct("P-MULTI", 1);

        Store store = newStore("S6");

        Aisle aisle = store.addAisle("A01", "Aisle A01", "Test Aisle", AisleLocation.floor);
        Shelf shelf1 = aisle.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test Shelf 1", Temperature.ambient);
        Inventory inv1 = shelf1.addInventory(
                "INV-P-MULTI-SH1", store.getId(), "A01", "SH1",
                10, 5, "P-MULTI", InventoryType.standard);
        store.addInventory(inv1);

        Shelf shelf2 = aisle.addShelf("SH2", "Shelf SH2", ShelfLevel.low, "Test Shelf 2", Temperature.ambient);
        Inventory inv2 = shelf2.addInventory(
                "INV-P-MULTI-SH2", store.getId(), "A01", "SH2",
                10, 5, "P-MULTI", InventoryType.standard);
        store.addInventory(inv2);

        attachBasketToStoreLocation(basket, customer, store, "A01");

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.removeProduct("P-MULTI", 1));

        assertEquals("Remove Product", ex.getAction());
        assertEquals("There Are Several Products In the Aisle", ex.getReason());
    }

    @Test
    @DisplayName("removeProduct throws when shelf capacity would be exceeded")
    public void testRemoveProductShelfCapacityExceededThrows() throws StoreException {
        Customer customer = newRegisteredCustomer("C11");
        Basket basket = newBasketWithCustomer(customer);

        // Basket currently holds 3 units
        basket.addProduct("P-CAP", 3);

        Store store = newStore("S7");
        // Shelf already nearly full: capacity 10, current count 9
        {
            Aisle aisleA01_5;
            try {
                aisleA01_5 = store.getAisle("A01");
            } catch (StoreException e) {
                aisleA01_5 = store.addAisle("A01", "Aisle A01", "Test aisle A01", AisleLocation.floor);
            }
            Shelf shelfSH1_5 = aisleA01_5.getShelf("SH1");
            if (shelfSH1_5 == null) {
                shelfSH1_5 = aisleA01_5.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test shelf SH1",
                        Temperature.ambient);
            }
            Inventory inv_P_CAP = shelfSH1_5.addInventory("INV-P-CAP-SH1", store.getId(), "A01", "SH1", 10, 9, "P-CAP",
                    InventoryType.standard);
            store.addInventory(inv_P_CAP);
        }
        attachBasketToStoreLocation(basket, customer, store, "A01");

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.removeProduct("P-CAP", 3));

        assertEquals("Remove Product", ex.getAction());
        assertEquals("There Is Not Enough Capacity on the Shelf", ex.getReason());
    }

    @Test
    @DisplayName("removeProduct success updates inventory and basket")
    public void testRemoveProductSuccessUpdatesInventoryAndBasket() throws StoreException {
        Customer customer = newRegisteredCustomer("C12");
        Basket basket = newBasketWithCustomer(customer);

        // Basket has 4 units of product
        basket.addProduct("P-OK", 4);

        Store store = newStore("S8");
        Inventory inventory;
        {
            Aisle aisleA01_6;
            try {
                aisleA01_6 = store.getAisle("A01");
            } catch (StoreException e) {
                aisleA01_6 = store.addAisle("A01", "Aisle A01", "Test aisle A01", AisleLocation.floor);
            }
            Shelf shelfSH1_6 = aisleA01_6.getShelf("SH1");
            if (shelfSH1_6 == null) {
                shelfSH1_6 = aisleA01_6.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test shelf SH1",
                        Temperature.ambient);
            }
            inventory = shelfSH1_6.addInventory("INV-P-OK-SH1", store.getId(), "A01", "SH1", 10, 6, "P-OK",
                    InventoryType.standard);
            store.addInventory(inventory);
        }
        attachBasketToStoreLocation(basket, customer, store, "A01");

        basket.removeProduct("P-OK", 4);

        assertFalse(basket.getProductMap().containsKey("P-OK"));
        assertEquals(10, inventory.getCount());
    }
}
