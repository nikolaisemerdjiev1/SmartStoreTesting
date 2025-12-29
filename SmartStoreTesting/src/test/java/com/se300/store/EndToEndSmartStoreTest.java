package com.se300.store;

import com.se300.store.controller.StoreController;
import com.se300.store.controller.UserController;
import com.se300.store.data.DataManager;
import com.se300.store.model.*;
import com.se300.store.repository.StoreRepository;
import com.se300.store.repository.UserRepository;
import com.se300.store.service.AuthenticationService;
import com.se300.store.service.StoreService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Tests for the Smart Store Application.
 * Tests the complete system including all layers: REST API, Controllers,
 * Services, Repositories, and Data.
 * Uses a clean Tomcat server (no sample data) to ensure test isolation.
 */
@DisplayName("Big Bang Integration Test - Complete System Testing")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndSmartStoreTest {

    /*
     * TODO: The following
     * 1. Achieve 100% Test Coverage
     * 2. Produce/Print Identical Results to Command Line DriverTest
     * -> testStoreScriptEndToEnd() executes store.script via CommandProcessor
     * using the same Command API as DriverTest.
     * 3. Produce SonarCube Quality and Coverage Report
     * -> With JaCoCo enabled in pom.xml, use:
     * mvn clean verify sonar:sonar
     * to push JaCoCo coverage + quality metrics to SonarQube.
     */

    private static DataManager dataManager;
    private static StoreRepository storeRepository;
    private static UserRepository userRepository;
    private static StoreService storeService;
    private static AuthenticationService authenticationService;
    private static Tomcat tomcat;

    @BeforeAll
    public static void setUpCompleteSystem() throws Exception {
        // Initialize data layer
        dataManager = DataManager.getInstance();
        dataManager.clear();

        // Clear static maps in StoreService to ensure clean state (no sample data)
        StoreService.clearAllMaps();

        // Initialize repositories
        storeRepository = new StoreRepository(dataManager);
        userRepository = new UserRepository(dataManager);

        // Initialize services
        storeService = new StoreService(storeRepository);
        authenticationService = new AuthenticationService(userRepository);

        // Initialize controllers
        StoreController storeController = new StoreController(storeService);
        UserController userController = new UserController(authenticationService);

        // Start clean Tomcat server (without sample data from SmartStoreApplication)
        tomcat = new Tomcat();
        tomcat.setPort(0); // Use dynamic port allocation
        tomcat.getConnector();

        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);

        // Register controllers
        Tomcat.addServlet(context, "storeController", storeController);
        context.addServletMappingDecoded("/api/v1/stores/*", "storeController");

        Tomcat.addServlet(context, "userController", userController);
        context.addServletMappingDecoded("/api/v1/users/*", "userController");

        tomcat.start();

        // Get the actual port assigned by the system
        int testPort = tomcat.getConnector().getLocalPort();

        // Configure RestAssured
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = testPort;

        // Wait for server to be ready
        Thread.sleep(1000);
    }

    @AfterEach
    public void cleanupBetweenTests() {
        // Don't clear data between tests since they're ordered and may depend on
        // previous test data
    }

    @AfterAll
    public static void tearDownCompleteSystem() throws Exception {
        // Stop Tomcat server
        if (tomcat != null) {
            try {
                tomcat.stop();
                tomcat.destroy();
                // Give server time to shut down completely
                Thread.sleep(1000);
            } catch (Exception e) {
                // Force cleanup even if stop fails
                System.err.println("Error stopping Tomcat: " + e.getMessage());
            }
        }

        // Clear all data
        if (dataManager != null) {
            dataManager.clear();
        }
        StoreService.clearAllMaps();
    }

    // ====================== E2E WORKFLOWS ======================

    @Test
    @Order(1)
    @DisplayName("E2E: Complete user registration and authentication workflow")
    public void testCompleteUserWorkflow() {
        String email = "e2e.user@example.com";

        // 1) Register user via REST API
        given()
                .queryParam("email", email)
                .queryParam("password", "secret123")
                .queryParam("name", "E2E User")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("email", equalTo(email))
                .body("name", equalTo("E2E User"));

        // 2) "Authenticate" by fetching user details
        when()
                .get("/api/v1/users/{email}", email)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("email", equalTo(email))
                .body("name", equalTo("E2E User"));

        // 3) Update user name
        given()
                .queryParam("name", "E2E User Updated")
                .when()
                .put("/api/v1/users/{email}", email)
                .then()
                .statusCode(200)
                .body("name", equalTo("E2E User Updated"));

        // 4) List all users and ensure our user is present
        when()
                .get("/api/v1/users")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("email", hasItem(email));

        // 5) Verify repository-level consistency
        assertTrue(userRepository.existsByEmail(email), "User repository should contain the E2E user");
        User user = userRepository.findByEmail(email).orElseThrow();
        assertEquals("E2E User Updated", user.getName());
    }

    @Test
    @Order(2)
    @DisplayName("E2E: Complete store provisioning and management workflow")
    public void testCompleteStoreWorkflow() throws StoreException {
        String storeId = "E2E_STORE_1";

        // 1) Create store via REST API
        given()
                .queryParam("storeId", storeId)
                .queryParam("description", "E2E Test Store")
                .queryParam("address", "100 Integration Way")
                .when()
                .post("/api/v1/stores")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(storeId))
                .body("description", equalTo("E2E Test Store"))
                .body("address", equalTo("100 Integration Way"));

        // 2) Get store via REST
        when()
                .get("/api/v1/stores/{id}", storeId)
                .then()
                .statusCode(200)
                .body("id", equalTo(storeId))
                .body("description", equalTo("E2E Test Store"));

        // 3) Update store via REST
        given()
                .queryParam("description", "E2E Test Store Updated")
                .queryParam("address", "101 Integration Way")
                .when()
                .put("/api/v1/stores/{id}", storeId)
                .then()
                .statusCode(200)
                .body("id", equalTo(storeId))
                .body("description", equalTo("E2E Test Store Updated"))
                .body("address", equalTo("101 Integration Way"));

        // 4) Verify service & repository consistency
        Store storeFromService = storeService.showStore(storeId, null);
        assertEquals("E2E Test Store Updated", storeFromService.getDescription());
        assertEquals("101 Integration Way", storeFromService.getAddress());

        assertTrue(storeRepository.existsById(storeId));
        Store storeFromRepo = storeRepository.findById(storeId).orElseThrow();
        assertEquals(storeFromService.getId(), storeFromRepo.getId());
        assertEquals(storeFromService.getDescription(), storeFromRepo.getDescription());
    }

    @Test
    @Order(3)
    @DisplayName("E2E: Complete store operations - aisles, shelves, products, inventory")
    public void testCompleteStoreOperations() throws StoreException {
        String storeId = "E2E_STORE_OPS";

        // Provision store
        Store store = storeService.provisionStore(storeId, "Ops Store", "200 Ops Street", null);
        assertNotNull(store);

        // Provision aisles
        Aisle aisleA = storeService.provisionAisle(storeId, "A1", "Aisle A1", "Aisle A1 Desc",
                AisleLocation.store_room, null);
        Aisle aisleB = storeService.provisionAisle(storeId, "B1", "Aisle B1", "Aisle B1 Desc",
                AisleLocation.floor, null);

        assertEquals("Aisle A1", aisleA.getName());
        assertEquals(AisleLocation.store_room, aisleA.getAisleLocation());
        assertEquals("Aisle B1", aisleB.getName());

        // Provision shelves
        Shelf shelf1 = storeService.provisionShelf(
                storeId, "A1", "S1", "Shelf 1", ShelfLevel.high,
                "Top shelf", Temperature.ambient, null);
        Shelf shelf2 = storeService.provisionShelf(
                storeId, "A1", "S2", "Shelf 2", ShelfLevel.low,
                "Bottom shelf", Temperature.frozen, null);

        assertEquals("S1", shelf1.getId());
        assertEquals(Temperature.ambient, shelf1.getTemperature());
        assertEquals(Temperature.frozen, shelf2.getTemperature());

        // Provision product
        Product product = storeService.provisionProduct(
                "E2E_PROD_1",
                "E2E Product",
                "E2E Product Desc",
                "1EA",
                "General",
                9.99,
                Temperature.ambient,
                null);
        assertEquals("E2E_PROD_1", product.getId());

        // Provision inventory for product on shelf
        Inventory inventory = storeService.provisionInventory(
                "E2E_INV_1",
                storeId,
                "A1",
                "S1",
                100, // capacity
                10, // initial count
                product.getId(),
                InventoryType.standard,
                null);

        assertEquals(10, inventory.getCount());
        assertEquals(100, inventory.getCapacity());
        assertEquals(InventoryType.standard, inventory.getType());

        // Show operations
        Aisle shownAisle = storeService.showAisle(storeId, "A1", null);
        assertEquals("Aisle A1", shownAisle.getName());

        Shelf shownShelf = storeService.showShelf(storeId, "A1", "S1", null);
        assertEquals("Shelf 1", shownShelf.getName());

        Product shownProduct = storeService.showProduct(product.getId(), null);
        assertEquals("E2E Product", shownProduct.getName());

        Inventory shownInventory = storeService.showInventory("E2E_INV_1", null);
        assertEquals(10, shownInventory.getCount());

        // Update inventory count (absolute count, service converts to delta)
        Inventory updatedInventory = storeService.updateInventory("E2E_INV_1", 25, null);
        assertEquals(25, updatedInventory.getCount());
    }

    @Test
    @Order(4)
    @DisplayName("E2E: Complete customer shopping workflow")
    public void testCompleteCustomerShoppingWorkflow() throws StoreException {
        String storeId = "E2E_STORE_SHOP";
        String productId = "E2E_PROD_SHOP";
        String customerId = "E2E_CUST_1";
        String basketId = "E2E_BASK_1";

        // Store + aisle + shelf
        storeService.provisionStore(storeId, "Shopping Store", "300 Shopping Ave", null);
        storeService.provisionAisle(storeId, "A1", "Shop Aisle", "Shop Aisle Desc", AisleLocation.floor, null);
        storeService.provisionShelf(
                storeId, "A1", "S1", "Shop Shelf", ShelfLevel.medium,
                "Shopping Shelf", Temperature.ambient, null);

        // Product + inventory
        Product product = storeService.provisionProduct(
                productId,
                "Basket Product",
                "For basket",
                "1EA",
                "Grocery",
                4.99,
                Temperature.ambient,
                null);
        storeService.provisionInventory(
                "E2E_INV_SHOP",
                storeId,
                "A1",
                "S1",
                50,
                20,
                productId,
                InventoryType.standard,
                null);

        // Customer
        Customer customer = storeService.provisionCustomer(
                customerId,
                "First",
                "Last",
                CustomerType.registered,
                "cust@example.com",
                "Customer Address",
                null);
        assertEquals(customerId, customer.getId());

        // Basket
        Basket basket = storeService.provisionBasket(basketId, null);
        assertEquals(basketId, basket.getId());

        // Assign basket to customer
        Basket assignedBasket = storeService.assignCustomerBasket(customerId, basketId, null);
        assertNotNull(assignedBasket.getCustomer());
        assertEquals(customerId, assignedBasket.getCustomer().getId());

        // Add product to basket
        Basket basketWithProduct = storeService.addBasketProduct(basketId, productId, 2, null);
        assertEquals(Integer.valueOf(2), basketWithProduct.getProductMap().get(productId));

        // Fetch customer basket
        Basket customerBasket = storeService.getCustomerBasket(customerId, null);
        assertEquals(basketId, customerBasket.getId());
        assertEquals(Integer.valueOf(2), customerBasket.getProductMap().get(productId));

        // Remove one unit
        Basket basketAfterRemove = storeService.removeBasketProduct(basketId, productId, 1, null);
        assertEquals(Integer.valueOf(1), basketAfterRemove.getProductMap().get(productId));

        // Clear basket
        Basket clearedBasket = storeService.clearBasket(basketId, null);
        assertTrue(clearedBasket.getProductMap().isEmpty(), "Basket should be empty after clear");
    }

    @Test
    @Order(5)
    @Timeout(value = 10, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("E2E: Device management and events")
    public void testCompleteDeviceWorkflow() throws StoreException {
        String storeId = "E2E_STORE_DEV";
        String aisleId = "DEV_A1";
        String sensorId = "DEV_SENSOR_1";
        String applianceId = "DEV_APPL_1";

        // Store + aisle
        storeService.provisionStore(storeId, "Device Store", "400 Device Rd", null);
        storeService.provisionAisle(storeId, aisleId, "Device Aisle", "Device Aisle Desc",
                AisleLocation.store_room, null);

        // Provision sensor device (SensorType.microphone)
        Device sensor = storeService.provisionDevice(sensorId, "Microphone Sensor", "microphone",
                storeId, aisleId, null);
        assertNotNull(sensor);
        assertEquals(sensorId, sensor.getId());

        // Provision appliance device (ApplianceType.speaker)
        Device appliance = storeService.provisionDevice(applianceId, "Speaker Appliance", "speaker",
                storeId, aisleId, null);
        assertNotNull(appliance);
        assertEquals(applianceId, appliance.getId());

        // Raise event on sensor
        assertDoesNotThrow(() -> storeService.raiseEvent(sensorId, "motion_detected", null));

        // Issue command to appliance
        assertDoesNotThrow(() -> storeService.issueCommand(applianceId, "volume_up", null));

        // Show device
        Device shownSensor = storeService.showDevice(sensorId, null);
        assertEquals("Microphone Sensor", shownSensor.getName());
    }

    @Test
    @Order(6)
    @DisplayName("E2E: Error handling across all layers")
    public void testCompleteErrorHandling() {
        // Service-level errors
        StoreException storeEx = assertThrows(StoreException.class,
                () -> storeService.showStore("NON_EXISTENT_STORE", null));
        assertEquals("Store Does Not Exist", storeEx.getReason());

        StoreException invEx = assertThrows(StoreException.class,
                () -> storeService.updateInventory("NO_SUCH_INV", 10, null));
        assertEquals("Inventory Does Not Exist", invEx.getReason());

        StoreException prodEx = assertThrows(StoreException.class,
                () -> storeService.showProduct("NO_SUCH_PROD", null));
        assertEquals("Product Does Not Exist", prodEx.getReason());

        StoreException deviceEx = assertThrows(StoreException.class,
                () -> storeService.raiseEvent("NO_SUCH_DEVICE", "event", null));
        assertEquals("Device Does Not Exist", deviceEx.getReason());
    }

    @Test
    @Order(7)
    @DisplayName("E2E: Data consistency across all layers")
    public void testDataConsistencyAcrossLayers() throws StoreException {
        String storeId = "E2E_STORE_DATA";
        String email = "data.user@example.com";

        // Create store via service (goes through repository)
        Store store = storeService.provisionStore(storeId, "Data Store", "500 Data Blvd", null);
        assertNotNull(store);

        // Repository must see the same store
        assertTrue(storeRepository.existsById(storeId));
        Store repoStore = storeRepository.findById(storeId).orElseThrow();
        assertEquals(store.getId(), repoStore.getId());

        // Create user via service
        authenticationService.registerUser(email, "pw", "Data User");
        assertTrue(userRepository.existsByEmail(email));
        User repoUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals("Data User", repoUser.getName());

        // Verify REST sees the same store and user
        when()
                .get("/api/v1/stores/{id}", storeId)
                .then()
                .statusCode(200)
                .body("id", equalTo(storeId))
                .body("description", equalTo("Data Store"));

        when()
                .get("/api/v1/users/{email}", email)
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("name", equalTo("Data User"));
    }

    @Test
    @Order(8)
    @DisplayName("E2E: REST API Controller - Store CRUD operations")
    public void testRestApiStoreOperations() {
        String storeId = "E2E_STORE_REST";

        // Create
        given()
                .queryParam("storeId", storeId)
                .queryParam("description", "REST Store")
                .queryParam("address", "600 REST Lane")
                .when()
                .post("/api/v1/stores")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(storeId));

        // Read single
        when()
                .get("/api/v1/stores/{id}", storeId)
                .then()
                .statusCode(200)
                .body("id", equalTo(storeId))
                .body("description", equalTo("REST Store"));

        // List all
        when()
                .get("/api/v1/stores")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", hasItem(storeId));

        // Update
        given()
                .queryParam("description", "REST Store Updated")
                .queryParam("address", "601 REST Lane")
                .when()
                .put("/api/v1/stores/{id}", storeId)
                .then()
                .statusCode(200)
                .body("description", equalTo("REST Store Updated"))
                .body("address", equalTo("601 REST Lane"));

        // Delete
        when()
                .delete("/api/v1/stores/{id}", storeId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(9)
    @DisplayName("E2E: REST API Controller - User CRUD operations")
    public void testRestApiUserOperations() {
        String email = "rest.user@example.com";

        // Create
        given()
                .queryParam("email", email)
                .queryParam("password", "restpw")
                .queryParam("name", "REST User")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .body("email", equalTo(email));

        // Read single
        when()
                .get("/api/v1/users/{email}", email)
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("name", equalTo("REST User"));

        // List all
        when()
                .get("/api/v1/users")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("email", hasItem(email));

        // Update
        given()
                .queryParam("name", "REST User Updated")
                .when()
                .put("/api/v1/users/{email}", email)
                .then()
                .statusCode(200)
                .body("name", equalTo("REST User Updated"));

        // Delete
        when()
                .delete("/api/v1/users/{email}", email)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(10)
    @DisplayName("E2E: REST API Controller - Error handling")
    public void testRestApiErrorHandling() {
        // Store not found
        when()
                .get("/api/v1/stores/{id}", "NO_SUCH_STORE")
                .then()
                .statusCode(404);

        // User not found
        when()
                .get("/api/v1/users/{email}", "no.such.user@example.com")
                .then()
                .statusCode(404);

        // Bad request: missing required params on store POST
        given()
                .queryParam("storeId", "BAD_STORE")
                // missing description and address
                .when()
                .post("/api/v1/stores")
                .then()
                .statusCode(400);

        // Conflict: duplicate user registration
        String email = "dup.e2e@example.com";
        given()
                .queryParam("email", email)
                .queryParam("password", "one")
                .queryParam("name", "Dup One")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201);

        given()
                .queryParam("email", email)
                .queryParam("password", "two")
                .queryParam("name", "Dup Two")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(409);
    }

    @Test
    @Order(11)
    @DisplayName("E2E: Final cleanup and deletion operations")
    public void testFinalCleanupOperations() throws StoreException {
        String storeId = "E2E_STORE_CLEANUP";
        String email = "cleanup.user@example.com";

        // Create store and user via services
        storeService.provisionStore(storeId, "Cleanup Store", "700 Cleanup Rd", null);
        authenticationService.registerUser(email, "cleanup", "Cleanup User");

        assertTrue(storeRepository.existsById(storeId));
        assertTrue(userRepository.existsByEmail(email));

        // Delete via REST
        when()
                .delete("/api/v1/stores/{id}", storeId)
                .then()
                .statusCode(204);

        when()
                .delete("/api/v1/users/{email}", email)
                .then()
                .statusCode(204);

        // Confirm removal in repositories / services
        assertFalse(storeRepository.existsById(storeId));
        assertFalse(userRepository.existsByEmail(email));

        StoreException ex = assertThrows(StoreException.class,
                () -> storeService.showStore(storeId, null));
        assertEquals("Store Does Not Exist", ex.getReason());
    }

    @Test
    @Order(12)
    @DisplayName("E2E: Complete store.script data processing with assertions")
    public void testStoreScriptEndToEnd() throws Exception {
        // Capture System.out so we can verify that DSL commands are processed
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream testOut = new PrintStream(baos, true, StandardCharsets.UTF_8);

        System.setOut(testOut);
        try {
            // Same behavior as DriverTest: run CommandProcessor on store.script
            Path scriptPath = Path.of(Objects.requireNonNull(
                    getClass().getResource("/store.script")).toURI());

            CommandProcessor processor = new CommandProcessor();

            assertDoesNotThrow(
                    () -> processor.processCommandFile(scriptPath.toString()),
                    "Processing store.script should not throw exceptions");

        } finally {
            System.setOut(originalOut);
            testOut.flush();
        }

        // Validate that some DSL commands were actually processed/printed
        String output = baos.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(">>> Processing DSL"),
                "CommandProcessor should log DSL processing lines");

        // Validate resulting domain state for the script-defined store
        // NOTE: We intentionally do NOT use IDs used earlier in this class (E2E_...)
        // so store.script can safely define store_123, aisles, etc.
        Store scriptStore = storeService.showStore("store_123", null);
        assertEquals("Chapman", scriptStore.getDescription());
        assertTrue(scriptStore.getAddress().contains("One University Drive"));

        // A few aisles defined in the script
        Aisle aisleA1 = storeService.showAisle("store_123", "aisle_A1", null);
        assertEquals("AISLE_A1", aisleA1.getName());

        Aisle aisleB2 = storeService.showAisle("store_123", "aisle_B2", null);
        assertEquals("AISLE_B2", aisleB2.getName());
    }
}
