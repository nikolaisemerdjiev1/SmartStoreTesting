package com.se300.store.model.unit;

import com.se300.store.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra unit tests focused on hitting the remaining uncovered instructions
 * in com.se300.store.model (Store, Shelf, Aisle, Inventory and the
 * supporting model types like Device, Basket, Customer, etc.).
 */
@DisplayName("Model additional coverage tests")
public class ModelAdditionalCoverageTest {

    // --- Small helper type to force Basket's 'aisle == null' branch ---
    private static class NullAisleStore extends Store {
        public NullAisleStore(String id, String address, String description) {
            super(id, address, description);
        }

        @Override
        public Aisle getAisle(String aisleNumber) {
            // Force Basket to see a null aisle instead of throwing from Store.getAisle
            return null;
        }
    }

    // ---------- Inventory ----------

    @Test
    @DisplayName("Inventory setters and updateInventory bounds")
    public void testInventorySettersAndUpdateInventoryBounds() throws StoreException {
        InventoryLocation location = new InventoryLocation("S1", "A01", "SH1");
        Inventory inventory = new Inventory("INV-1", location, 10, 5, "P-1", InventoryType.standard);

        inventory.setId("INV-2");
        inventory.setInventoryLocation(new InventoryLocation("S1", "A02", "SH2"));
        inventory.setCapacity(20);
        inventory.setCount(3);
        inventory.setProductId("P-2");
        inventory.setType(InventoryType.flexible);

        assertEquals("INV-2", inventory.getId());
        assertEquals("P-2", inventory.getProductId());
        assertEquals(InventoryType.flexible, inventory.getType());
        assertEquals(3, inventory.getCount());

        // Happy path update
        inventory.updateInventory(5);
        assertEquals(8, inventory.getCount());

        // Out-of-bounds update throws
        StoreException ex = assertThrows(StoreException.class,
                () -> inventory.updateInventory(50));
        assertEquals("Update Inventory", ex.getAction());
    }

    // ---------- Shelf ----------

    @Test
    @DisplayName("Shelf setters and addInventory validation")
    public void testShelfSettersAndAddInventoryValidation() throws StoreException {
        Shelf shelf = new Shelf("SH1", "Initial", ShelfLevel.high, "Initial desc", Temperature.ambient);

        shelf.setId("SH2");
        shelf.setName("Updated");
        shelf.setLevel(ShelfLevel.low);
        shelf.setDescription("Updated desc");
        shelf.setTemperature(Temperature.frozen);

        assertEquals("SH2", shelf.getId());
        assertEquals("Updated", shelf.getName());
        assertEquals(ShelfLevel.low, shelf.getLevel());
        assertEquals("Updated desc", shelf.getDescription());
        assertEquals(Temperature.frozen, shelf.getTemperature());

        // Valid inventory
        Inventory ok = shelf.addInventory("INV-OK", "S1", "A01", "SH2", 10, 5, "P-OK", InventoryType.standard);
        assertNotNull(ok);

        // Invalid inventory (count > capacity) hits bounds check
        StoreException ex = assertThrows(StoreException.class,
                () -> shelf.addInventory("INV-BAD", "S1", "A01", "SH2", 5, 10, "P-BAD", InventoryType.standard));
        assertEquals("Add Inventory", ex.getAction());
    }

    // ---------- Aisle ----------

    @Test
    @DisplayName("Aisle setters and duplicate shelf handling")
    public void testAisleSettersAndDuplicateShelfHandling() throws StoreException {
        Aisle aisle = new Aisle("A01", "Original", "Orig desc", AisleLocation.floor);

        aisle.setNumber("A02");
        aisle.setName("Updated Aisle");
        aisle.setDescription("Updated desc");
        aisle.setAisleLocation(AisleLocation.store_room);

        assertEquals("A02", aisle.getNumber());
        assertEquals("Updated Aisle", aisle.getName());
        assertEquals("Updated desc", aisle.getDescription());
        assertEquals(AisleLocation.store_room, aisle.getAisleLocation());

        Shelf shelf1 = aisle.addShelf("SH1", "Shelf 1", ShelfLevel.high, "Shelf desc", Temperature.ambient);
        assertNotNull(shelf1);
        assertTrue(aisle.getShelfMap().containsKey("SH1"));

        // Same level duplicate should trigger "Shelf Already Exists at This Level"
        StoreException ex = assertThrows(StoreException.class,
                () -> aisle.addShelf("SH2", "Shelf 2", ShelfLevel.high, "Another", Temperature.ambient));
        assertEquals("Add Shelf", ex.getAction());
    }

    // ---------- Store ----------

    @Test
    @DisplayName("Store duplicate entities and remove unknown customer")
    public void testStoreDuplicateEntitiesAndRemoveUnknownCustomer() throws StoreException {
        Store store = new Store("S-D1", "999 Test Rd", "Coverage Store");

        InventoryLocation loc = new InventoryLocation(store.getId(), "A01", "SH1");
        Inventory inventory = new Inventory("INV-1", loc, 10, 5, "P-1", InventoryType.standard);
        store.addInventory(inventory);

        StoreException invEx = assertThrows(StoreException.class,
                () -> store.addInventory(inventory));
        assertEquals("Add Inventory", invEx.getAction());

        Customer customer = new Customer("C-1", "Test", "User",
                CustomerType.registered, "test@example.com", "123 Main St");
        store.addCustomer(customer);

        StoreException custEx = assertThrows(StoreException.class,
                () -> store.addCustomer(customer));
        assertEquals("Add Customer", custEx.getAction());

        Appliance device = new Appliance("D-1", "Device",
                new StoreLocation(store.getId(), "A01"), "GENERIC");
        store.addDevice(device);

        StoreException devEx = assertThrows(StoreException.class,
                () -> store.addDevice(device));
        assertEquals("Add Device", devEx.getAction());

        // Remove a customer that is not present – branch in removeCustomer
        Customer other = new Customer("C-MISSING", "Other", "User",
                CustomerType.registered, "other@example.com", "456 Elsewhere");
        store.removeCustomer(other);

        // Touch toString to include the tail of the method
        assertTrue(store.toString().contains("Store{"));
    }

    // ---------- Device via Appliance (covers Device setters / getters) ----------

    @Test
    @DisplayName("Device setters and getters are exercised via Appliance")
    public void testDeviceGettersAndSettersViaAppliance() {
        StoreLocation originalLocation = new StoreLocation("S1", "A01");
        Appliance appliance = new Appliance("D-OLD", "Old Name", originalLocation, "OLD_TYPE");

        StoreLocation newLocation = new StoreLocation("S2", "A02");

        appliance.setId("D-NEW");
        appliance.setName("New Name");
        appliance.setStoreLocation(newLocation);
        appliance.setType("NEW_TYPE");

        assertEquals("D-NEW", appliance.getId());
        assertEquals("New Name", appliance.getName());
        assertEquals(newLocation, appliance.getStoreLocation());
        assertEquals("NEW_TYPE", appliance.getType());
    }

    // ---------- Basket special branches ----------

    @Test
    @DisplayName("addProduct works in basket-only mode when store/location are unknown")
    public void testBasketAddProductWithUnknownStoreOrLocation() throws StoreException {
        Customer customer = new Customer("C-B1", "First", "Last",
                CustomerType.registered, "c1@example.com", "Addr");
        Basket basket = new Basket("B-B1");
        basket.setCustomer(customer);

        // store is null and storeLocation is null – should use basket-only branch
        basket.addProduct("P1", 2);

        assertEquals(2, basket.getProductMap().get("P1"));
    }

    @Test
    @DisplayName("addProduct throws Aisle Does Not Exist when Store returns null aisle")
    public void testBasketAddProductAisleDoesNotExistViaNullStore() {
        Customer customer = new Customer("C-B2", "First", "Last",
                CustomerType.registered, "c2@example.com", "Addr");
        Basket basket = new Basket("B-B2");
        basket.setCustomer(customer);

        Store store = new NullAisleStore("S-NULL", "Addr", "Null aisle store");
        basket.setStore(store);
        customer.setStoreLocation(new StoreLocation(store.getId(), "A01"));

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.addProduct("P-NULL", 1));

        assertEquals("Add Product", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getReason());
    }

    @Test
    @DisplayName("removeProduct in basket-only mode updates and cleans productMap")
    public void testBasketRemoveProductWithUnknownStoreOrLocation() throws StoreException {
        Customer customer = new Customer("C-B3", "First", "Last",
                CustomerType.registered, "c3@example.com", "Addr");
        Basket basket = new Basket("B-B3");
        basket.setCustomer(customer);

        // Add using basket-only branch
        basket.addProduct("P1", 2);
        assertEquals(2, basket.getProductMap().get("P1"));

        // Remove using basket-only branch – should drop entry when count reaches zero
        basket.removeProduct("P1", 2);
        assertFalse(basket.getProductMap().containsKey("P1"));
    }

    @Test
    @DisplayName("removeProduct throws Aisle Does Not Exist when Store returns null aisle")
    public void testBasketRemoveProductAisleDoesNotExistViaNullStore() throws StoreException {
        Customer customer = new Customer("C-B4", "First", "Last",
                CustomerType.registered, "c4@example.com", "Addr");
        Basket basket = new Basket("B-B4");
        basket.setCustomer(customer);

        // Put something in the basket first (basket-only add)
        basket.addProduct("P-NULL", 1);

        Store store = new NullAisleStore("S-NULL-2", "Addr", "Null aisle store 2");
        basket.setStore(store);
        customer.setStoreLocation(new StoreLocation(store.getId(), "A01"));

        StoreException ex = assertThrows(StoreException.class,
                () -> basket.removeProduct("P-NULL", 1));

        assertEquals("Remove Product", ex.getAction());
        assertEquals("Aisle Does Not Exist", ex.getReason());
    }

    @Test
    @DisplayName("removeProduct with real store and inventory restores counts and removes zero entries")
    public void testBasketRemoveProductWithInventoryAndZeroCleanup() throws StoreException {
        // Build a minimal store / aisle / shelf / inventory graph
        Store store = new Store("S-INV", "123 Main St", "Inventory Store");
        Aisle aisle = store.addAisle("A01", "Aisle A01", "Test aisle", AisleLocation.floor);
        Shelf shelf = aisle.addShelf("SH1", "Shelf SH1", ShelfLevel.high, "Test shelf", Temperature.ambient);

        Inventory inventory = shelf.addInventory("INV-P1", store.getId(), "A01", "SH1",
                10, 6, "P-INV", InventoryType.standard);
        store.addInventory(inventory);

        Customer customer = new Customer("C-B5", "First", "Last",
                CustomerType.registered, "c5@example.com", "Addr");
        Basket basket = new Basket("B-B5");
        basket.setCustomer(customer);
        basket.setStore(store);
        customer.setStoreLocation(new StoreLocation(store.getId(), "A01"));

        // Take 4 units from the shelf into the basket
        basket.addProduct("P-INV", 4);
        assertEquals(4, basket.getProductMap().get("P-INV"));
        assertEquals(2, inventory.getCount());

        // Remove all 4 units – should restore inventory and clear basket entry
        basket.removeProduct("P-INV", 4);
        assertFalse(basket.getProductMap().containsKey("P-INV"));
        assertEquals(6, inventory.getCount());
    }

    // ---------- Customer ----------

    @Test
    @DisplayName("Customer setId and setEmail update fields")
    public void testCustomerSettersForIdAndEmail() {
        Customer customer = new Customer("C-ORIG", "First", "Last",
                CustomerType.registered, "orig@example.com", "Addr");

        customer.setId("C-NEW");
        customer.setEmail("new@example.com");

        assertEquals("C-NEW", customer.getId());
        assertEquals("new@example.com", customer.getEmail());
    }

    // ---------- CommandException ----------

    @Test
    @DisplayName("CommandException setters update underlying fields")
    public void testCommandExceptionSetters() {
        CommandException ex = new CommandException("OLD_CMD", "Old reason");

        assertEquals("OLD_CMD", ex.getCommand());
        assertEquals("Old reason", ex.getReason());

        ex.setCommand("NEW_CMD");
        ex.setReason("New reason");
        ex.setLineNumber(42);

        assertEquals("NEW_CMD", ex.getCommand());
        assertEquals("New reason", ex.getReason());
        assertEquals(42, ex.getLineNumber());
    }

    // ---------- InventoryLocation ----------

    @Test
    @DisplayName("InventoryLocation shelfId getter and setter")
    public void testInventoryLocationShelfIdAccessors() {
        InventoryLocation location = new InventoryLocation("S1", "A01", "SH1");
        assertEquals("SH1", location.getShelfId());

        location.setShelfId("SH2");
        assertEquals("SH2", location.getShelfId());

        assertTrue(location.toString().contains("InventoryLocation"));
    }

    // ---------- CommandProcessor ----------

    @Test
    @DisplayName("CommandProcessor safely handles IOExceptions when reading command file")
    public void testCommandProcessorIOExceptionHandling() {
        CommandProcessor processor = new CommandProcessor();

        // Use a path that is almost guaranteed not to exist so Files.lines throws
        processor.processCommandFile("this-file-should-not-exist-12345.dsl");
    }

    // ---------- User ----------

    @Test
    @DisplayName("User no-arg constructor and setters")
    public void testUserDefaultConstructorAndSetters() {
        User user = new User(); // exercise no-arg constructor

        user.setEmail("user@example.com");
        user.setPassword("secret");
        user.setName("Test User");

        assertEquals("user@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("Test User", user.getName());
    }
}
