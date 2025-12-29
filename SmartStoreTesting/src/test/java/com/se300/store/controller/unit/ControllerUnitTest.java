package com.se300.store.controller.unit;

import com.se300.store.controller.StoreController;
import com.se300.store.controller.UserController;
import com.se300.store.model.Store;
import com.se300.store.model.User;
import com.se300.store.service.AuthenticationService;
import com.se300.store.service.StoreService;
import io.restassured.RestAssured;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Store and User controllers using Mockito and RestAssured.
 * These tests mock the service layer to test controller logic in isolation.
 */
@DisplayName("Controller Mock Tests - Unit Testing with Mockito")
@ExtendWith(MockitoExtension.class)
public class ControllerUnitTest {

    @Mock
    private StoreService storeService;

    @Mock
    private AuthenticationService authenticationService;

    private static Tomcat tomcat;
    private static final int TEST_PORT = 8081; // Different port from integration tests
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;

    private StoreController storeController;
    private UserController userController;

    @BeforeEach
    public void setUp() throws LifecycleException {
        // Create controllers with mocked services
        storeController = new StoreController(storeService);
        userController = new UserController(authenticationService);

        // Start embedded Tomcat server with mocked controllers
        tomcat = new Tomcat();
        tomcat.setPort(TEST_PORT);
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

        // Configure RestAssured
        RestAssured.baseURI = BASE_URL;
        RestAssured.port = TEST_PORT;
    }

    @AfterEach
    public void tearDown() throws LifecycleException {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
        // Reset mocks after each test
        reset(storeService, authenticationService);
    }

    // ==================== STORE CONTROLLER MOCK TESTS ====================

    @Test
    @DisplayName("Mock: Create store - verify service call")
    public void testCreateStoreWithMock() throws Exception {
        Store store = new Store("S1", "Addr", "Desc");
        when(storeService.provisionStore(eq("S1"), eq("Desc"), eq("Addr"), isNull()))
                .thenReturn(store);

        given()
                .queryParam("storeId", "S1")
                .queryParam("description", "Desc")
                .queryParam("address", "Addr")
                .when()
                .post("/api/v1/stores")
                .then()
                .statusCode(201)
                .body("id", equalTo("S1"))
                .body("description", equalTo("Desc"));

        verify(storeService, times(1))
                .provisionStore(eq("S1"), eq("Desc"), eq("Addr"), isNull());
    }

    @Test
    @DisplayName("Mock: Get all stores - verify service call")
    public void testGetAllStoresWithMock() throws Exception {
        Store s1 = new Store("S1", "Addr1", "Desc1");
        Store s2 = new Store("S2", "Addr2", "Desc2");
        Collection<Store> stores = Arrays.asList(s1, s2);

        when(storeService.getAllStores()).thenReturn(stores);

        when()
                .get("/api/v1/stores")
                .then()
                .statusCode(200)
                .body("id", hasItems("S1", "S2"));

        verify(storeService, times(1)).getAllStores();
    }

    @Test
    @DisplayName("Mock: Get store by ID - verify service call")
    public void testGetStoreByIdWithMock() throws Exception {
        Store s = new Store("S10", "Addr10", "Desc10");
        when(storeService.showStore(eq("S10"), isNull())).thenReturn(s);

        when()
                .get("/api/v1/stores/{id}", "S10")
                .then()
                .statusCode(200)
                .body("id", equalTo("S10"));

        verify(storeService, times(1)).showStore(eq("S10"), isNull());
    }

    @Test
    @DisplayName("Mock: Update store - verify service call")
    public void testUpdateStoreWithMock() throws Exception {
        Store updated = new Store("S20", "NewAddr", "NewDesc");
        when(storeService.updateStore(eq("S20"), eq("NewDesc"), eq("NewAddr"))).thenReturn(updated);

        given()
                .queryParam("description", "NewDesc")
                .queryParam("address", "NewAddr")
                .when()
                .put("/api/v1/stores/{id}", "S20")
                .then()
                .statusCode(200)
                .body("id", equalTo("S20"))
                .body("description", equalTo("NewDesc"));

        verify(storeService, times(1))
                .updateStore(eq("S20"), eq("NewDesc"), eq("NewAddr"));
    }

    @Test
    @DisplayName("Mock: Delete store - verify service call")
    public void testDeleteStoreWithMock() throws Exception {
        doNothing().when(storeService).deleteStore("S30");

        when()
                .delete("/api/v1/stores/{id}", "S30")
                .then()
                .statusCode(204);

        verify(storeService, times(1)).deleteStore("S30");
    }

    @Test
    @DisplayName("Mock: Store error handling - service throws exception")
    public void testStoreErrorHandlingWithMock() throws Exception {
        when(storeService.showStore(eq("unknown"), isNull()))
                .thenThrow(new RuntimeException("Store not found"));

        when()
                .get("/api/v1/stores/{id}", "unknown")
                .then()
                // controller should translate to 404 or 500; the implementation
                // in StoreController uses 404 on StoreException and 500 on others.
                // Here we just assert it's not 2xx.
                .statusCode(anyOf(equalTo(404), equalTo(500)));

        verify(storeService, times(1)).showStore(eq("unknown"), isNull());
    }

    // ==================== USER CONTROLLER MOCK TESTS ====================

    @Test
    @DisplayName("Mock: Register user - verify service call")
    public void testRegisterUserWithMock() throws Exception {
        User user = new User("user@example.com", "pw", "User");
        when(authenticationService.userExists("user@example.com")).thenReturn(false);
        when(authenticationService.registerUser("user@example.com", "pw", "User")).thenReturn(user);

        given()
                .queryParam("email", "user@example.com")
                .queryParam("password", "pw")
                .queryParam("name", "User")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .body("email", equalTo("user@example.com"));

        verify(authenticationService).userExists("user@example.com");
        verify(authenticationService).registerUser("user@example.com", "pw", "User");
    }

    @Test
    @DisplayName("Mock: Get all users - verify service call")
    public void testGetAllUsersWithMock() throws Exception {
        User u1 = new User("u1@example.com", "p1", "U1");
        User u2 = new User("u2@example.com", "p2", "U2");
        when(authenticationService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        when()
                .get("/api/v1/users")
                .then()
                .statusCode(200)
                .body("email", hasItems("u1@example.com", "u2@example.com"));

        verify(authenticationService).getAllUsers();
    }

    @Test
    @DisplayName("Mock: Get user by email - verify service call")
    public void testGetUserByEmailWithMock() throws Exception {
        User u = new User("get@example.com", "pw", "Name");
        when(authenticationService.getUserByEmail("get@example.com")).thenReturn(u);

        when()
                .get("/api/v1/users/{email}", "get@example.com")
                .then()
                .statusCode(200)
                .body("email", equalTo("get@example.com"));

        verify(authenticationService).getUserByEmail("get@example.com");
    }

    @Test
    @DisplayName("Mock: Get user by email - user not found")
    public void testGetUserByEmailNotFoundWithMock() throws Exception {
        when(authenticationService.getUserByEmail("missing@example.com")).thenReturn(null);

        when()
                .get("/api/v1/users/{email}", "missing@example.com")
                .then()
                .statusCode(404);

        verify(authenticationService).getUserByEmail("missing@example.com");
    }

    @Test
    @DisplayName("Mock: Update user - verify service call")
    public void testUpdateUserWithMock() throws Exception {
        User updated = new User("update@example.com", "newpw", "New Name");
        when(authenticationService.updateUser("update@example.com", "newpw", "New Name"))
                .thenReturn(updated);

        given()
                .queryParam("password", "newpw")
                .queryParam("name", "New Name")
                .when()
                .put("/api/v1/users/{email}", "update@example.com")
                .then()
                .statusCode(200)
                .body("name", equalTo("New Name"));

        verify(authenticationService).updateUser("update@example.com", "newpw", "New Name");
    }

    @Test
    @DisplayName("Mock: Delete user - verify service call")
    public void testDeleteUserWithMock() throws Exception {
        when(authenticationService.deleteUser("delete@example.com")).thenReturn(true);

        when()
                .delete("/api/v1/users/{email}", "delete@example.com")
                .then()
                .statusCode(204);

        verify(authenticationService).deleteUser("delete@example.com");
    }

    @Test
    @DisplayName("Mock: Delete user - user not found")
    public void testDeleteUserNotFoundWithMock() throws Exception {
        when(authenticationService.deleteUser("missing@example.com")).thenReturn(false);

        when()
                .delete("/api/v1/users/{email}", "missing@example.com")
                .then()
                .statusCode(404);

        verify(authenticationService).deleteUser("missing@example.com");
    }

    @Test
    @DisplayName("Mock: Register duplicate user - verify conflict handling")
    public void testRegisterDuplicateUserWithMock() throws Exception {
        when(authenticationService.userExists("dup@example.com")).thenReturn(true);

        given()
                .queryParam("email", "dup@example.com")
                .queryParam("password", "pw")
                .queryParam("name", "Dup")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(409);

        verify(authenticationService).userExists("dup@example.com");
        verify(authenticationService, never()).registerUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Mock: Verify no unexpected service calls")
    public void testNoUnexpectedServiceCalls() throws Exception {
        // After setup, before any HTTP calls, there should be no interactions.
        verifyNoInteractions(storeService, authenticationService);
    }
}
