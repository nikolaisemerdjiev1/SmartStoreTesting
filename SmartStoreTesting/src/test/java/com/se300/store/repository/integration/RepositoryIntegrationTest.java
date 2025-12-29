package com.se300.store.repository.integration;

import com.se300.store.data.DataManager;
import com.se300.store.model.Store;
import com.se300.store.model.User;
import com.se300.store.repository.StoreRepository;
import com.se300.store.repository.UserRepository;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RepositoryIntegrationTest is designed to perform integration tests
 * for repository classes, ensuring their functionality and verifying
 * operations such as persistence, updates, deletions, and concurrency.
 */
@DisplayName("Repository Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RepositoryIntegrationTest {

    private static DataManager dataManager;
    private static StoreRepository storeRepository;
    private static UserRepository userRepository;

    @BeforeAll
    public static void setUpClass() {
        dataManager = DataManager.getInstance();
        dataManager.clear();

        // Initialize stores map so StoreRepository does not see null
        dataManager.put("stores", new HashMap<String, Store>());

        // Repositories (UserRepository will initialize default users if needed)
        storeRepository = new StoreRepository(dataManager);
        userRepository = new UserRepository(dataManager);
    }

    // ==================== STORE TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Integration: Save multiple stores and verify persistence")
    public void testSaveMultipleStores() {
        Store store1 = new Store("S001", "123 Main St", "First store");
        Store store2 = new Store("S002", "456 Oak Ave", "Second store");

        storeRepository.save(store1);
        storeRepository.save(store2);

        Map<String, Store> stores = storeRepository.findAll();
        assertEquals(2, stores.size(), "There should be 2 stores persisted");
        assertTrue(stores.containsKey("S001"));
        assertTrue(stores.containsKey("S002"));

        Store s1 = stores.get("S001");
        assertNotNull(s1);
        assertEquals("123 Main St", s1.getAddress());
        assertEquals("First store", s1.getDescription());
    }

    @Test
    @Order(2)
    @DisplayName("Integration: Update store and verify changes")
    public void testUpdateStore() {
        Store existing = storeRepository.findById("S001")
                .orElseThrow(() -> new AssertionError("Store S001 should exist"));

        existing.setDescription("Updated first store");
        existing.setAddress("123 Main St, Suite 100");
        storeRepository.save(existing);

        Store updated = storeRepository.findById("S001")
                .orElseThrow(() -> new AssertionError("Updated store S001 should still exist"));

        assertEquals("Updated first store", updated.getDescription());
        assertEquals("123 Main St, Suite 100", updated.getAddress());

        assertEquals(2, storeRepository.findAll().size(), "Store count should remain 2");
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Delete store and verify removal")
    public void testDeleteStore() {
        assertTrue(storeRepository.existsById("S002"), "Store S002 should exist before deletion");

        storeRepository.delete("S002");

        assertFalse(storeRepository.existsById("S002"), "Store S002 should be deleted");
        Map<String, Store> stores = storeRepository.findAll();
        assertEquals(1, stores.size(), "There should be exactly 1 store after deletion");
        assertTrue(stores.containsKey("S001"));
    }

    // ==================== USER TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Integration: Register multiple users and verify")
    public void testRegisterMultipleUsers() {
        // At this point UserRepository has its default users (e.g. admin & user)
        int initialUserCount = userRepository.findAll().size();

        User user1 = new User("alice@example.com", "password1", "Alice");
        User user2 = new User("bob@example.com", "password2", "Bob");

        userRepository.save(user1);
        userRepository.save(user2);

        Map<String, User> users = userRepository.findAll();
        assertEquals(initialUserCount + 2, users.size(),
                "There should be 2 additional users persisted");

        assertTrue(users.containsKey("alice@example.com"));
        assertTrue(users.containsKey("bob@example.com"));

        User alice = userRepository.findByEmail("alice@example.com")
                .orElseThrow(() -> new AssertionError("Alice should be found by email"));
        assertEquals("Alice", alice.getName());
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Update user and verify changes")
    public void testUpdateUser() {
        int initialUserCount = userRepository.findAll().size();

        User alice = userRepository.findByEmail("alice@example.com")
                .orElseThrow(() -> new AssertionError("Alice should already exist"));

        alice.setName("Alice Wonderland");
        alice.setPassword("newPassword1");

        userRepository.save(alice);

        User updatedAlice = userRepository.findByEmail("alice@example.com")
                .orElseThrow(() -> new AssertionError("Updated Alice should still exist"));

        assertEquals("Alice Wonderland", updatedAlice.getName());
        assertEquals("newPassword1", updatedAlice.getPassword());

        // No extra user should be created by update
        assertEquals(initialUserCount, userRepository.findAll().size());
    }

    // ==================== CROSS-REPO & MULTI-OP TESTS ====================

    @Test
    @Order(6)
    @DisplayName("Integration: Cross-repository data consistency")
    public void testCrossRepositoryConsistency() {
        Map<String, Store> stores = storeRepository.findAll();
        Map<String, User> users = userRepository.findAll();

        assertEquals(1, stores.size(), "Expected 1 store in repository");
        assertTrue(stores.containsKey("S001"));

        // We know at least the default users plus Alice & Bob exist.
        assertTrue(users.size() >= 4, "Expected at least 4 users in repository");
        assertTrue(users.containsKey("alice@example.com"));
        assertTrue(users.containsKey("bob@example.com"));

        assertTrue(dataManager.keys().contains("stores"), "DataManager should contain 'stores' key");
        assertTrue(dataManager.keys().contains("users"), "DataManager should contain 'users' key");
        assertTrue(dataManager.size() >= 2, "DataManager should have at least 2 top-level entries");
    }

    @Test
    @Order(7)
    @DisplayName("Integration: Concurrent repository operations (multi-operation scenario)")
    public void testConcurrentOperations() {
        int initialStoreCount = storeRepository.findAll().size();
        int initialUserCount = userRepository.findAll().size();

        // Add several new stores
        for (int i = 0; i < 5; i++) {
            String id = "S_LOOP_" + i;
            Store store = new Store(id, "Loop Address " + i, "Loop Store " + i);
            storeRepository.save(store);
        }

        // Add several new users
        for (int i = 0; i < 5; i++) {
            String email = "loop_user_" + i + "@example.com";
            User user = new User(email, "loopPass" + i, "Loop User " + i);
            userRepository.save(user);
        }

        Map<String, Store> storesAfterSaves = storeRepository.findAll();
        Map<String, User> usersAfterSaves = userRepository.findAll();

        assertEquals(initialStoreCount + 5, storesAfterSaves.size(),
                "Store count should increase by 5 after loop saves");
        assertEquals(initialUserCount + 5, usersAfterSaves.size(),
                "User count should increase by 5 after loop saves");

        // Now delete some of them
        storeRepository.delete("S_LOOP_0");
        storeRepository.delete("S_LOOP_1");
        userRepository.delete("loop_user_0@example.com");
        userRepository.delete("loop_user_1@example.com");

        Map<String, Store> storesFinal = storeRepository.findAll();
        Map<String, User> usersFinal = userRepository.findAll();

        assertEquals(initialStoreCount + 3, storesFinal.size(),
                "Two loop stores deleted, so +3 net over initial");
        assertEquals(initialUserCount + 3, usersFinal.size(),
                "Two loop users deleted, so +3 net over initial");

        assertFalse(storesFinal.containsKey("S_LOOP_0"));
        assertFalse(storesFinal.containsKey("S_LOOP_1"));
        assertFalse(usersFinal.containsKey("loop_user_0@example.com"));
        assertFalse(usersFinal.containsKey("loop_user_1@example.com"));
    }
}
