package com.se300.store.data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Manager simulates Persistence Storage, use this class as a placeholder
 * for persistence layer
 *
 * @author Sergey L. Sundukovskiy
 * @version 1.0
 * @since 2025-11-06
 */
public class DataManager {

    // Singleton instance
    private static volatile DataManager instance;

    // "Fake database" – shared in-memory map
    private final Map<String, Object> store;

    /**
     * Private constructor so only getInstance() can create it.
     */
    private DataManager() {
        this.store = new ConcurrentHashMap<>();
    }

    /**
     * Get singleton instance of DataManager.
     * This makes the data "persist" for the lifetime of the JVM.
     */
    public static DataManager getInstance() {
        if (instance == null) {
            synchronized (DataManager.class) {
                if (instance == null) {
                    instance = new DataManager();
                }
            }
        }
        return instance;
    }

    /**
     * Put a value into the store.
     *
     * @param key   storage key
     * @param value value to store
     */
    public <T> void put(String key, T value) {
        store.put(key, value);
    }

    /**
     * Get a value from the store.
     *
     * @param key storage key
     * @param <T> expected type
     * @return value or null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) store.get(key);
    }

    /**
     * Check if key exists.
     */
    public boolean containsKey(String key) {
        return store.containsKey(key);
    }

    /**
     * Remove a key/value pair.
     *
     * @param key storage key
     * @param <T> value type
     * @return removed value or null
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(String key) {
        return (T) store.remove(key);
    }

    /**
     * Remove all entries.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Get all keys.
     */
    public Set<String> keys() {
        return store.keySet();
    }

    /**
     * Number of entries.
     */
    public int size() {
        return store.size();
    }
}
