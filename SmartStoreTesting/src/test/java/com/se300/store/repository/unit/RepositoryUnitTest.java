package com.se300.store.repository.unit;

import com.se300.store.data.DataManager;
import com.se300.store.model.Store;
import com.se300.store.model.User;
import com.se300.store.repository.StoreRepository;
import com.se300.store.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Repository classes including StoreRepository and
 * UserRepository.
 * This test class uses JUnit 5 and Mockito frameworks to verify the expected
 * behavior
 * of the repository operations with mocked dependencies.
 */
@DisplayName("Repository Unit Tests")
@ExtendWith(MockitoExtension.class)
public class RepositoryUnitTest {

    // Unit Tests for the Smart Store Repositories

    @Mock
    private DataManager dataManager;

    private StoreRepository storeRepository;
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        // Recreate repositories before each test using the mocked DataManager
        storeRepository = new StoreRepository(dataManager);
        userRepository = new UserRepository(dataManager);
    }

    // ==================== STORE REPOSITORY TESTS ====================

    @Test
    @DisplayName("Test StoreRepository save with mocked DataManager")
    public void testStoreRepositorySave() {
        Map<String, Store> storeMap = new HashMap<>();
        // Whenever the repository asks for "stores", return our in-memory map
        when(dataManager.get("stores")).thenReturn(storeMap);

        Store store = new Store("S001", "123 Main St", "Test Store");
        storeRepository.save(store);

        assertEquals(1, storeMap.size(), "Store map should contain one entry");
        assertTrue(storeMap.containsKey("S001"));
        assertSame(store, storeMap.get("S001"));

        verify(dataManager, atLeastOnce()).get("stores");
        // No need to verify put() here; StoreRepository uses existing map
    }

    @Test
    @DisplayName("Test StoreRepository findById with mocked DataManager")
    public void testStoreRepositoryFindById() {
        Map<String, Store> storeMap = new HashMap<>();
        Store store = new Store("S100", "456 Oak Ave", "FindById Store");
        storeMap.put("S100", store);
        when(dataManager.get("stores")).thenReturn(storeMap);

        Optional<Store> result = storeRepository.findById("S100");

        assertTrue(result.isPresent(), "Store should be found");
        assertSame(store, result.get());
        verify(dataManager, atLeastOnce()).get("stores");
    }

    @Test
    @DisplayName("Test StoreRepository existsById with mocked DataManager")
    public void testStoreRepositoryExistsById() {
        Map<String, Store> storeMap = new HashMap<>();
        storeMap.put("S200", new Store("S200", "789 Pine Rd", "Exists Store"));
        when(dataManager.get("stores")).thenReturn(storeMap);

        assertTrue(storeRepository.existsById("S200"), "Store S200 should exist");
        assertFalse(storeRepository.existsById("S999"), "Store S999 should not exist");

        verify(dataManager, atLeastOnce()).get("stores");
    }

    @Test
    @DisplayName("Test StoreRepository delete with mocked DataManager")
    public void testStoreRepositoryDelete() {
        Map<String, Store> storeMap = new HashMap<>();
        storeMap.put("S300", new Store("S300", "Delete St", "To Delete"));
        storeMap.put("S301", new Store("S301", "Keep St", "To Keep"));
        when(dataManager.get("stores")).thenReturn(storeMap);

        storeRepository.delete("S300");

        assertFalse(storeMap.containsKey("S300"), "Store S300 should be removed");
        assertTrue(storeMap.containsKey("S301"), "Store S301 should remain");

        verify(dataManager, atLeastOnce()).get("stores");
    }

    @Test
    @DisplayName("Test StoreRepository findAll with mocked DataManager")
    public void testStoreRepositoryFindAll() {
        Map<String, Store> storeMap = new HashMap<>();
        storeMap.put("S400", new Store("S400", "Addr1", "Store 1"));
        storeMap.put("S401", new Store("S401", "Addr2", "Store 2"));
        when(dataManager.get("stores")).thenReturn(storeMap);

        Map<String, Store> result = storeRepository.findAll();

        assertEquals(2, result.size(), "Result should contain 2 stores");
        assertTrue(result.containsKey("S400"));
        assertTrue(result.containsKey("S401"));

        // Verify that it's a copy, not the same map instance
        assertNotSame(storeMap, result, "findAll should return a defensive copy");

        verify(dataManager, atLeastOnce()).get("stores");
    }

    // ==================== USER REPOSITORY TESTS ====================

    @Test
    @DisplayName("Test UserRepository save with mocked DataManager")
    public void testUserRepositorySave() {
        Map<String, User> userMap = new HashMap<>();
        when(dataManager.get("users")).thenReturn(userMap);

        User user = new User("alice@example.com", "password", "Alice");
        userRepository.save(user);

        assertEquals(1, userMap.size(), "User map should contain one entry");
        assertTrue(userMap.containsKey("alice@example.com"));
        assertSame(user, userMap.get("alice@example.com"));

        verify(dataManager, atLeastOnce()).get("users");
    }

    @Test
    @DisplayName("Test UserRepository findByEmail with mocked DataManager")
    public void testUserRepositoryFindByEmail() {
        Map<String, User> userMap = new HashMap<>();
        User user = new User("bob@example.com", "secret", "Bob");
        userMap.put("bob@example.com", user);
        when(dataManager.get("users")).thenReturn(userMap);

        Optional<User> result = userRepository.findByEmail("bob@example.com");

        assertTrue(result.isPresent(), "User should be found by email");
        assertSame(user, result.get());

        verify(dataManager, atLeastOnce()).get("users");
    }

    @Test
    @DisplayName("Test UserRepository existsByEmail with mocked DataManager")
    public void testUserRepositoryExistsByEmail() {
        Map<String, User> userMap = new HashMap<>();
        userMap.put("carol@example.com", new User("carol@example.com", "pwd", "Carol"));
        when(dataManager.get("users")).thenReturn(userMap);

        assertTrue(userRepository.existsByEmail("carol@example.com"), "User Carol should exist");
        assertFalse(userRepository.existsByEmail("missing@example.com"), "Missing user should not exist");

        verify(dataManager, atLeastOnce()).get("users");
    }

    @Test
    @DisplayName("Test UserRepository delete with mocked DataManager")
    public void testUserRepositoryDelete() {
        Map<String, User> userMap = new HashMap<>();
        userMap.put("dave@example.com", new User("dave@example.com", "pwd", "Dave"));
        userMap.put("erin@example.com", new User("erin@example.com", "pwd", "Erin"));
        when(dataManager.get("users")).thenReturn(userMap);

        userRepository.delete("dave@example.com");

        assertFalse(userMap.containsKey("dave@example.com"), "Dave should be deleted");
        assertTrue(userMap.containsKey("erin@example.com"), "Erin should remain");

        verify(dataManager, atLeastOnce()).get("users");
    }

    @Test
    @DisplayName("Test UserRepository findAll with mocked DataManager")
    public void testUserRepositoryFindAll() {
        Map<String, User> userMap = new HashMap<>();
        userMap.put("x@example.com", new User("x@example.com", "xpwd", "X"));
        userMap.put("y@example.com", new User("y@example.com", "ypwd", "Y"));
        when(dataManager.get("users")).thenReturn(userMap);

        Map<String, User> result = userRepository.findAll();

        assertEquals(2, result.size(), "Result should contain 2 users");
        assertTrue(result.containsKey("x@example.com"));
        assertTrue(result.containsKey("y@example.com"));
        assertNotSame(userMap, result, "findAll should return a defensive copy");

        verify(dataManager, atLeastOnce()).get("users");
    }

    // ==================== NULL DATA MANAGER BEHAVIOR ====================

    @Test
    @DisplayName("Test Repository operations with null DataManager response")
    public void testRepositoryWithNullDataManager() {
        // Simulate DataManager returning null for both keys
        when(dataManager.get("stores")).thenReturn(null);
        when(dataManager.get("users")).thenReturn(null);

        // StoreRepository has a null-guard in getStoresMap() and should create a new
        // map
        Map<String, Store> storesResult = storeRepository.findAll();
        assertNotNull(storesResult, "StoreRepository.findAll should not return null");
        assertTrue(storesResult.isEmpty(), "StoreRepository.findAll should return an empty map when none exists");

        // And it should have called put() to initialize the map
        verify(dataManager, atLeastOnce()).put(eq("stores"), any(Map.class));

        // UserRepository has a null-guard in getUsersMap() and should create a new map
        Map<String, User> usersResult = userRepository.findAll();
        assertNotNull(usersResult, "UserRepository.findAll should not return null");
        assertTrue(usersResult.isEmpty(), "UserRepository.findAll should return an empty map when none exists");

        // And it should have called put() to initialize the map
        verify(dataManager, atLeastOnce()).put(eq("users"), any(Map.class));
    }
}
