package com.se300.store.servlet;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JsonHelperTest {

    // Simple POJO to exercise toJson / fromJson
    static class Dummy {
        private String name;
        private int value;

        Dummy(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    @DisplayName("toJson / fromJson round-trip for a simple object")
    void toJsonFromJson_roundTrip() {
        Dummy original = new Dummy("test", 42);

        String json = JsonHelper.toJson(original);
        Dummy restored = JsonHelper.fromJson(json, Dummy.class);

        assertNotNull(restored);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getValue(), restored.getValue());
    }

    @Test
    @DisplayName("LocalDate is serialized and deserialized in ISO-8601 format")
    void localDate_serializationAndDeserialization() {
        Gson gson = JsonHelper.getGson();
        LocalDate date = LocalDate.of(2025, 1, 2);

        String json = gson.toJson(date);
        assertEquals("\"2025-01-02\"", json);

        LocalDate parsed = gson.fromJson(json, LocalDate.class);
        assertEquals(date, parsed);
    }

    @Test
    @DisplayName("LocalDateTime is serialized and deserialized in ISO-8601 format with time")
    void localDateTime_serializationAndDeserialization() {
        Gson gson = JsonHelper.getGson();
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 2, 13, 14, 15);

        String json = gson.toJson(dateTime);
        assertEquals("\"2025-01-02T13:14:15\"", json);

        LocalDateTime parsed = gson.fromJson(json, LocalDateTime.class);
        assertEquals(dateTime, parsed);
    }

    @Test
    @DisplayName("getGson returns the same configured instance")
    void getGson_returnsSingletonInstance() {
        Gson gson1 = JsonHelper.getGson();
        Gson gson2 = JsonHelper.getGson();

        assertNotNull(gson1);
        assertSame(gson1, gson2);
    }
}
