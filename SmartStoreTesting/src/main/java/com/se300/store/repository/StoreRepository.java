package com.se300.store.repository;

import com.se300.store.data.DataManager;
import com.se300.store.model.Store;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Store Repository represents the store data access layer
 *
 * @author Sergey L. Sundukovskiy
 * @version 1.0
 * @since 2025-11-06
 */
public class StoreRepository {

    private static final String STORES_KEY = "stores";
    private final DataManager dataManager;

    public StoreRepository(DataManager dataManager) {
        this.dataManager = dataManager;

        // Make sure there is always a map for stores in DataManager
        if (!dataManager.containsKey(STORES_KEY)) {
            dataManager.put(STORES_KEY, new HashMap<String, Store>());
        }
    }

    /**
     * Find store by ID.
     *
     * @param storeId ID of the store
     * @return Optional containing the store if found, otherwise empty
     */
    public Optional<Store> findById(String storeId) {
        Map<String, Store> stores = getStoresMap();
        return Optional.ofNullable(stores.get(storeId));
    }

    /**
     * Save or update a store.
     *
     * @param store store entity to persist
     */
    public void save(Store store) {
        Map<String, Store> stores = getStoresMap();
        stores.put(store.getId(), store);
        dataManager.put(STORES_KEY, stores);
    }

    /**
     * Check if a store exists by ID.
     *
     * @param storeId ID of the store
     * @return true if store exists, false otherwise
     */
    public boolean existsById(String storeId) {
        Map<String, Store> stores = getStoresMap();
        return stores.containsKey(storeId);
    }

    /**
     * Delete a store by ID.
     *
     * @param storeId ID of the store to delete
     */
    public void delete(String storeId) {
        Map<String, Store> stores = getStoresMap();
        stores.remove(storeId);
        dataManager.put(STORES_KEY, stores);
    }

    /**
     * Get all stores.
     *
     * @return copy of all stores keyed by store ID
     */
    public Map<String, Store> findAll() {
        return new HashMap<>(getStoresMap());
    }

    /**
     * Helper method to get the stores map from DataManager.
     * Lazily initializes the map if it does not already exist.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Store> getStoresMap() {
        Map<String, Store> stores = dataManager.get(STORES_KEY);
        if (stores == null) {
            stores = new HashMap<>();
            dataManager.put(STORES_KEY, stores);
        }
        return stores;
    }
}
