package com.se300.store.controller.internalmockserver;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * A test class for verifying internal Smart Store API calls using a mock
 * server.
 * Ensures the functionality of multiple API endpoints and tests various
 * scenarios
 * such as successful requests, error handling, and unauthorized access.
 */
@DisplayName("Internal Mock Server Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InternalMockServerTest {

    private static ClientAndServer mockServer;
    private static final int MOCK_SERVER_PORT = 8888;
    private static final String BASE_URL = "http://localhost:" + MOCK_SERVER_PORT;

    @BeforeAll
    public static void setUpMockServer() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
    }

    @AfterAll
    public static void tearDownMockServer() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @BeforeEach
    public void setUp() {
        mockServer.reset();
    }

    @Test
    @Order(1)
    @DisplayName("Mock Server: Test internal store provisioning API endpoint")
    public void testInternalStoreProvisioningAPI() {
        MockServerClient client = new MockServerClient("localhost", MOCK_SERVER_PORT);

        client.when(
                request()
                        .withMethod("POST")
                        .withPath("/internal/stores/provision"))
                .respond(
                        response()
                                .withStatusCode(201)
                                .withHeader(new Header("Content-Type", "application/json"))
                                .withBody(
                                        "{\"id\":\"S1\",\"address\":\"123 Main St\",\"description\":\"Provisioned Store\"}"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"id\":\"S1\",\"address\":\"123 Main St\",\"description\":\"Provisioned Store\"}")
                .when()
                .post(BASE_URL + "/internal/stores/provision")
                .then()
                .statusCode(201)
                .body("id", equalTo("S1"))
                .body("address", equalTo("123 Main St"));
    }

    @Test
    @Order(2)
    @DisplayName("Mock Server: Test internal store retrieval API endpoint")
    public void testInternalStoreRetrievalAPI() {
        MockServerClient client = new MockServerClient("localhost", MOCK_SERVER_PORT);

        client.when(
                request()
                        .withMethod("GET")
                        .withPath("/internal/stores/S1"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader(new Header("Content-Type", "application/json"))
                                .withBody(
                                        "{\"id\":\"S1\",\"address\":\"123 Main St\",\"description\":\"Provisioned Store\"}"));

        given()
                .when()
                .get(BASE_URL + "/internal/stores/{id}", "S1")
                .then()
                .statusCode(200)
                .body("id", equalTo("S1"))
                .body("description", equalTo("Provisioned Store"));
    }

    @Test
    @Order(3)
    @DisplayName("Mock Server: Test internal user registration API endpoint")
    public void testInternalUserRegistrationAPI() {
        MockServerClient client = new MockServerClient("localhost", MOCK_SERVER_PORT);

        client.when(
                request()
                        .withMethod("POST")
                        .withPath("/internal/users/register"))
                .respond(
                        response()
                                .withStatusCode(201)
                                .withHeader(new Header("Content-Type", "application/json"))
                                .withBody("{\"email\":\"user@internal\",\"name\":\"Internal User\"}"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"user@internal\",\"password\":\"pw\",\"name\":\"Internal User\"}")
                .when()
                .post(BASE_URL + "/internal/users/register")
                .then()
                .statusCode(201)
                .body("email", equalTo("user@internal"))
                .body("name", equalTo("Internal User"));
    }

    @Test
    @Order(4)
    @DisplayName("Mock Server: Test internal authentication API endpoint")
    public void testInternalAuthenticationAPI() {
        MockServerClient client = new MockServerClient("localhost", MOCK_SERVER_PORT);

        client.when(
                request()
                        .withMethod("POST")
                        .withPath("/internal/auth/login")
                        .withHeader("Authorization", "Basic dXNlcjpzZWNyZXQ=") // "user:secret"
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(new Header("Content-Type", "application/json"))
                        .withBody("{\"authenticated\":true}"));

        given()
                .header("Authorization", "Basic dXNlcjpzZWNyZXQ=")
                .when()
                .post(BASE_URL + "/internal/auth/login")
                .then()
                .statusCode(200)
                .body("authenticated", equalTo(true));
    }

    @Test
    @Order(5)
    @DisplayName("Mock Server: Test internal error handling - 404 Not Found")
    public void testInternalErrorHandling() {
        MockServerClient client = new MockServerClient("localhost", MOCK_SERVER_PORT);

        client.when(
                request()
                        .withMethod("GET")
                        .withPath("/internal/stores/unknown"))
                .respond(
                        response()
                                .withStatusCode(404)
                                .withBody("{\"error\":\"Not Found\"}"));

        given()
                .when()
                .get(BASE_URL + "/internal/stores/unknown")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    @DisplayName("Mock Server: Test internal unauthorized access - 401")
    public void testInternalUnauthorizedAccess() {
        MockServerClient client = new MockServerClient("localhost", MOCK_SERVER_PORT);

        client.when(
                request()
                        .withMethod("GET")
                        .withPath("/internal/admin"))
                .respond(
                        response()
                                .withStatusCode(401)
                                .withBody("{\"error\":\"Unauthorized\"}"));

        given()
                .when()
                .get(BASE_URL + "/internal/admin")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(7)
    @DisplayName("Mock Server: Verify request was received")
    public void testMockServerRequestVerification() {
        MockServerClient client = new MockServerClient("localhost", MOCK_SERVER_PORT);

        client.when(
                request()
                        .withMethod("POST")
                        .withPath("/internal/events"))
                .respond(
                        response()
                                .withStatusCode(202)
                                .withBody("{\"status\":\"accepted\"}"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"type\":\"STORE_UPDATE\"}")
                .when()
                .post(BASE_URL + "/internal/events")
                .then()
                .statusCode(202);

        client.verify(
                request()
                        .withMethod("POST")
                        .withPath("/internal/events"));
    }
}
