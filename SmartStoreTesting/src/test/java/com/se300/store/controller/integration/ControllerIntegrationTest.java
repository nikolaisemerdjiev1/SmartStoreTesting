package com.se300.store.controller.integration;

import com.se300.store.SmartStoreApplication;
import com.se300.store.data.DataManager;
import com.se300.store.service.StoreService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Store and User controllers using RestAssured.
 * These tests validate the complete REST API by making HTTP requests to the
 * running server.
 */
@DisplayName("Controller Integration Tests - REST API")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ControllerIntegrationTest {

    private static SmartStoreApplication application;
    private static final int TEST_PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;

    @BeforeAll
    public static void setUpClass() throws Exception {
        // Clear any existing data
        DataManager.getInstance().clear();

        // Start the embedded Tomcat server
        application = new SmartStoreApplication();
        application.startNonBlocking();

        // Configure RestAssured
        RestAssured.baseURI = BASE_URL;
        RestAssured.port = TEST_PORT;

        // Wait for server to be ready
        Thread.sleep(2000);
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
        // Stop the server after all tests
        if (application != null) {
            application.stop();
        }
    }

    @BeforeEach
    public void setUp() {
        // Clear data between tests
        DataManager.getInstance().clear();
        StoreService.clearAllMaps();
    }

    // ==================== STORE CONTROLLER TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Integration: Create store via REST API")
    public void testCreateStore() {
        given()
                .queryParam("storeId", "S1")
                .queryParam("description", "Integration Test Store")
                .queryParam("address", "123 Main St")
                .when()
                .post("/api/v1/stores")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo("S1"))
                .body("description", equalTo("Integration Test Store"))
                .body("address", equalTo("123 Main St"));
    }

    @Test
    @Order(2)
    @DisplayName("Integration: Get all stores via REST API")
    public void testGetAllStores() {
        // create two stores
        given().queryParam("storeId", "S1").queryParam("description", "Store1").queryParam("address", "Addr1")
                .when().post("/api/v1/stores").then().statusCode(201);

        given().queryParam("storeId", "S2").queryParam("description", "Store2").queryParam("address", "Addr2")
                .when().post("/api/v1/stores").then().statusCode(201);

        when()
                .get("/api/v1/stores")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", hasItems("S1", "S2"));
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Get store by ID via REST API")
    public void testGetStoreById() {
        given().queryParam("storeId", "S10").queryParam("description", "Store10").queryParam("address", "Addr10")
                .when().post("/api/v1/stores").then().statusCode(201);

        when()
                .get("/api/v1/stores/{id}", "S10")
                .then()
                .statusCode(200)
                .body("id", equalTo("S10"))
                .body("description", equalTo("Store10"));
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Update store via REST API")
    public void testUpdateStore() {
        given().queryParam("storeId", "S20").queryParam("description", "OldDesc").queryParam("address", "OldAddr")
                .when().post("/api/v1/stores").then().statusCode(201);

        given()
                .queryParam("description", "NewDesc")
                .queryParam("address", "NewAddr")
                .when()
                .put("/api/v1/stores/{id}", "S20")
                .then()
                .statusCode(200)
                .body("id", equalTo("S20"))
                .body("description", equalTo("NewDesc"))
                .body("address", equalTo("NewAddr"));
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Delete store via REST API")
    public void testDeleteStore() {
        given().queryParam("storeId", "S30").queryParam("description", "Desc").queryParam("address", "Addr")
                .when().post("/api/v1/stores").then().statusCode(201);

        when()
                .delete("/api/v1/stores/{id}", "S30")
                .then()
                .statusCode(204);

        when()
                .get("/api/v1/stores/{id}", "S30")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    @DisplayName("Integration: Complete store CRUD workflow via REST API")
    public void testStoreCompleteWorkflow() {
        // Create
        given().queryParam("storeId", "SWF").queryParam("description", "WF Store").queryParam("address", "WF Addr")
                .when().post("/api/v1/stores").then().statusCode(201);

        // Read
        when().get("/api/v1/stores/{id}", "SWF")
                .then().statusCode(200).body("id", equalTo("SWF"));

        // Update
        given().queryParam("description", "WF Store Updated").queryParam("address", "WF Addr Updated")
                .when().put("/api/v1/stores/{id}", "SWF")
                .then().statusCode(200).body("description", equalTo("WF Store Updated"));

        // Delete
        when().delete("/api/v1/stores/{id}", "SWF")
                .then().statusCode(204);

        // Confirm deleted
        when().get("/api/v1/stores/{id}", "SWF")
                .then().statusCode(404);
    }

    // ==================== USER CONTROLLER TESTS ====================

    @Test
    @Order(7)
    @DisplayName("Integration: Register user via REST API")
    public void testRegisterUser() {
        given()
                .queryParam("email", "user1@example.com")
                .queryParam("password", "secret")
                .queryParam("name", "User One")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("email", equalTo("user1@example.com"))
                .body("name", equalTo("User One"));
    }

    @Test
    @Order(8)
    @DisplayName("Integration: Get all users via REST API")
    public void testGetAllUsers() {
        given().queryParam("email", "u1@example.com").queryParam("password", "p1").queryParam("name", "U1")
                .when().post("/api/v1/users").then().statusCode(201);

        given().queryParam("email", "u2@example.com").queryParam("password", "p2").queryParam("name", "U2")
                .when().post("/api/v1/users").then().statusCode(201);

        when()
                .get("/api/v1/users")
                .then()
                .statusCode(200)
                .body("email", hasItems("u1@example.com", "u2@example.com"));
    }

    @Test
    @Order(9)
    @DisplayName("Integration: Get user by email via REST API")
    public void testGetUserByEmail() {
        String email = "getme@example.com";

        given().param("email", email).param("password", "pw").param("name", "Name")
                .when().post("/api/v1/users").then().statusCode(201);

        when()
                .get("/api/v1/users/{email}", email)
                .then()
                .statusCode(200)
                .body("email", equalTo(email));
    }

    @Test
    @Order(10)
    @DisplayName("Integration: Update user via REST API")
    public void testUpdateUser() {
        String email = "update@example.com";

        given().param("email", email).param("password", "old").param("name", "Old Name")
                .when().post("/api/v1/users").then().statusCode(201);

        given()
                .param("password", "newpass")
                .param("name", "New Name")
                .when()
                .put("/api/v1/users/{email}", email)
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("name", equalTo("New Name"));
    }

    @Test
    @Order(11)
    @DisplayName("Integration: Delete user via REST API")
    public void testDeleteUser() {
        String email = "delete@example.com";

        given().param("email", email).param("password", "pw").param("name", "Name")
                .when().post("/api/v1/users").then().statusCode(201);

        when()
                .delete("/api/v1/users/{email}", email)
                .then()
                .statusCode(204);

        when()
                .get("/api/v1/users/{email}", email)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(12)
    @DisplayName("Integration: Complete user CRUD workflow via REST API")
    public void testUserCompleteWorkflow() {
        String email = "workflow@example.com";

        // Register
        given().param("email", email).param("password", "pw").param("name", "Name")
                .when().post("/api/v1/users").then().statusCode(201);

        // Read
        when().get("/api/v1/users/{email}", email)
                .then().statusCode(200).body("email", equalTo(email));

        // Update
        given().param("password", "newpw").param("name", "New Name")
                .when().put("/api/v1/users/{email}", email)
                .then().statusCode(200).body("name", equalTo("New Name"));

        // Delete
        when().delete("/api/v1/users/{email}", email)
                .then().statusCode(204);

        // Confirm deleted
        when().get("/api/v1/users/{email}", email)
                .then().statusCode(404);
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @Order(13)
    @DisplayName("Integration: Test error handling - Missing parameters")
    public void testErrorHandlingMissingParameters() {
        // Missing user parameters
        when()
                .post("/api/v1/users")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(14)
    @DisplayName("Integration: Test error handling - User not found")
    public void testErrorHandlingUserNotFound() {
        when()
                .get("/api/v1/users/{email}", "doesnotexist@example.com")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(15)
    @DisplayName("Integration: Test error handling - Duplicate user")
    public void testErrorHandlingDuplicateUser() {
        String email = "dup@example.com";

        given().param("email", email).param("password", "pw").param("name", "Name")
                .when().post("/api/v1/users").then().statusCode(201);

        // Second registration should conflict
        given().param("email", email).param("password", "pw2").param("name", "Name2")
                .when().post("/api/v1/users")
                .then()
                .statusCode(409);
    }
}
