package com.se300.store.controller.unit;

import com.se300.store.controller.StoreController;
import com.se300.store.controller.UserController;
import com.se300.store.model.StoreException;
import com.se300.store.service.AuthenticationService;
import com.se300.store.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ControllerAdditionalCoverageTest {

    @Mock
    private StoreService storeService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private TestableStoreController storeController;
    private TestableUserController userController;

    private StringWriter responseWriter;

    @BeforeEach
    public void setUp() throws Exception {
        storeController = new TestableStoreController(storeService);
        userController = new TestableUserController(authenticationService);

        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    // Test helper subclasses that expose protected servlet methods as public
    // wrappers
    private static class TestableStoreController extends StoreController {
        public TestableStoreController(StoreService service) {
            super(service);
        }

        public void callDoGet(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doGet(req, resp);
        }

        public void callDoPost(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doPost(req, resp);
        }

        public void callDoPut(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doPut(req, resp);
        }

        public void callDoDelete(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doDelete(req, resp);
        }
    }

    private static class TestableUserController extends UserController {
        public TestableUserController(com.se300.store.service.AuthenticationService auth) {
            super(auth);
        }

        public void callDoGet(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doGet(req, resp);
        }

        public void callDoPost(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doPost(req, resp);
        }

        public void callDoPut(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doPut(req, resp);
        }

        public void callDoDelete(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            super.doDelete(req, resp);
        }
    }

    // ===== StoreController tests =====

    @Test
    @DisplayName("StoreController GET: list all stores when no ID in path")
    public void testStoreGetAllStoresWhenNoId() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        when(storeService.getAllStores()).thenReturn(Collections.emptyList());

        storeController.callDoGet(request, response);

        verify(storeService).getAllStores();
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("StoreController GET: StoreException translates to 404")
    public void testStoreGetStoreExceptionNotFound() throws Exception {
        when(request.getPathInfo()).thenReturn("/S404");
        when(storeService.showStore(eq("S404"), isNull()))
                .thenThrow(new StoreException("Show Store", "Not Found"));

        storeController.callDoGet(request, response);

        verify(storeService).showStore(eq("S404"), isNull());
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("StoreController GET: unexpected exception translates to 500")
    public void testStoreGetUnexpectedException() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        when(storeService.getAllStores()).thenThrow(new RuntimeException("boom"));

        storeController.callDoGet(request, response);

        verify(storeService).getAllStores();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("StoreController POST: missing parameters returns 400")
    public void testStorePostMissingParams() throws Exception {
        when(request.getParameter("storeId")).thenReturn(null);
        when(request.getParameter("description")).thenReturn("Desc");
        when(request.getParameter("address")).thenReturn("Addr");

        storeController.callDoPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(storeService, never())
                .provisionStore(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("StoreController POST: StoreException translates to 400")
    public void testStorePostStoreException() throws Exception {
        when(request.getParameter("storeId")).thenReturn("S1");
        when(request.getParameter("description")).thenReturn("Desc");
        when(request.getParameter("address")).thenReturn("Addr");

        when(storeService.provisionStore(eq("S1"), eq("Desc"), eq("Addr"), isNull()))
                .thenThrow(new StoreException("Provision Store", "Store Already Exists"));

        storeController.callDoPost(request, response);

        verify(storeService).provisionStore(eq("S1"), eq("Desc"), eq("Addr"), isNull());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("StoreController POST: unexpected exception translates to 500")
    public void testStorePostUnexpectedException() throws Exception {
        when(request.getParameter("storeId")).thenReturn("S1");
        when(request.getParameter("description")).thenReturn("Desc");
        when(request.getParameter("address")).thenReturn("Addr");

        when(storeService.provisionStore(eq("S1"), eq("Desc"), eq("Addr"), isNull()))
                .thenThrow(new RuntimeException("boom"));

        storeController.callDoPost(request, response);

        verify(storeService).provisionStore(eq("S1"), eq("Desc"), eq("Addr"), isNull());
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("StoreController PUT: missing store ID in path returns 400")
    public void testStorePutMissingId() throws Exception {
        when(request.getPathInfo()).thenReturn(null);

        storeController.callDoPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(storeService, never()).updateStore(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("StoreController PUT: both description and address missing returns 400")
    public void testStorePutMissingDescriptionAndAddress() throws Exception {
        when(request.getPathInfo()).thenReturn("/S1");
        when(request.getParameter("description")).thenReturn(null);
        when(request.getParameter("address")).thenReturn(null);

        storeController.callDoPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(storeService, never()).updateStore(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("StoreController PUT: StoreException translates to 404")
    public void testStorePutStoreException() throws Exception {
        when(request.getPathInfo()).thenReturn("/S1");
        when(request.getParameter("description")).thenReturn("NewDesc");
        when(request.getParameter("address")).thenReturn("NewAddr");

        when(storeService.updateStore(eq("S1"), eq("NewDesc"), eq("NewAddr")))
                .thenThrow(new StoreException("Update Store", "Store Not Found"));

        storeController.callDoPut(request, response);

        verify(storeService).updateStore(eq("S1"), eq("NewDesc"), eq("NewAddr"));
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("StoreController PUT: unexpected exception translates to 500")
    public void testStorePutUnexpectedException() throws Exception {
        when(request.getPathInfo()).thenReturn("/S1");
        when(request.getParameter("description")).thenReturn("NewDesc");
        when(request.getParameter("address")).thenReturn("NewAddr");

        when(storeService.updateStore(eq("S1"), eq("NewDesc"), eq("NewAddr")))
                .thenThrow(new RuntimeException("boom"));

        storeController.callDoPut(request, response);

        verify(storeService).updateStore(eq("S1"), eq("NewDesc"), eq("NewAddr"));
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("StoreController DELETE: missing store ID returns 400")
    public void testStoreDeleteMissingId() throws Exception {
        when(request.getPathInfo()).thenReturn(null);

        storeController.callDoDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(storeService, never()).deleteStore(anyString());
    }

    @Test
    @DisplayName("StoreController DELETE: StoreException translates to 404")
    public void testStoreDeleteStoreException() throws Exception {
        when(request.getPathInfo()).thenReturn("/S1");
        doThrow(new StoreException("Delete Store", "Store Not Found"))
                .when(storeService).deleteStore("S1");

        storeController.callDoDelete(request, response);

        verify(storeService).deleteStore("S1");
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("StoreController DELETE: unexpected exception translates to 500")
    public void testStoreDeleteUnexpectedException() throws Exception {
        when(request.getPathInfo()).thenReturn("/S1");
        doThrow(new RuntimeException("boom")).when(storeService).deleteStore("S1");

        storeController.callDoDelete(request, response);

        verify(storeService).deleteStore("S1");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // ===== UserController tests =====

    @Test
    @DisplayName("UserController GET: list all users when no email in path")
    public void testUserGetAllUsersWhenNoEmail() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        when(authenticationService.getAllUsers()).thenReturn(Collections.emptyList());

        userController.callDoGet(request, response);

        verify(authenticationService).getAllUsers();
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("UserController GET: unexpected exception translates to 500")
    public void testUserGetUnexpectedException() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        when(authenticationService.getAllUsers()).thenThrow(new RuntimeException("boom"));

        userController.callDoGet(request, response);

        verify(authenticationService).getAllUsers();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("UserController POST: missing parameters returns 400")
    public void testUserPostMissingParams() throws Exception {
        when(request.getParameter("email")).thenReturn(null);
        when(request.getParameter("password")).thenReturn("pw");
        when(request.getParameter("name")).thenReturn("Name");

        userController.callDoPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(authenticationService, never())
                .registerUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UserController POST: unexpected exception translates to 500")
    public void testUserPostUnexpectedException() throws Exception {
        when(request.getParameter("email")).thenReturn("user@example.com");
        when(request.getParameter("password")).thenReturn("pw");
        when(request.getParameter("name")).thenReturn("Name");

        when(authenticationService.userExists("user@example.com")).thenReturn(false);
        when(authenticationService.registerUser("user@example.com", "pw", "Name"))
                .thenThrow(new RuntimeException("boom"));

        userController.callDoPost(request, response);

        verify(authenticationService).userExists("user@example.com");
        verify(authenticationService).registerUser("user@example.com", "pw", "Name");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("UserController PUT: missing email in path returns 400")
    public void testUserPutMissingEmail() throws Exception {
        when(request.getPathInfo()).thenReturn(null);

        userController.callDoPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(authenticationService, never())
                .updateUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UserController PUT: missing password and name returns 400")
    public void testUserPutMissingPasswordAndName() throws Exception {
        when(request.getPathInfo()).thenReturn("/user@example.com");
        when(request.getParameter("password")).thenReturn(null);
        when(request.getParameter("name")).thenReturn(null);

        userController.callDoPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(authenticationService, never())
                .updateUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UserController PUT: user not found translates to 404")
    public void testUserPutUserNotFound() throws Exception {
        when(request.getPathInfo()).thenReturn("/missing@example.com");
        when(request.getParameter("password")).thenReturn("pw");
        when(request.getParameter("name")).thenReturn("Name");

        when(authenticationService.updateUser("missing@example.com", "pw", "Name"))
                .thenReturn(null);

        userController.callDoPut(request, response);

        verify(authenticationService).updateUser("missing@example.com", "pw", "Name");
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("UserController PUT: unexpected exception translates to 500")
    public void testUserPutUnexpectedException() throws Exception {
        when(request.getPathInfo()).thenReturn("/boom@example.com");
        when(request.getParameter("password")).thenReturn("pw");
        when(request.getParameter("name")).thenReturn("Name");

        when(authenticationService.updateUser("boom@example.com", "pw", "Name"))
                .thenThrow(new RuntimeException("boom"));

        userController.callDoPut(request, response);

        verify(authenticationService).updateUser("boom@example.com", "pw", "Name");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("UserController DELETE: missing email returns 400")
    public void testUserDeleteMissingEmail() throws Exception {
        when(request.getPathInfo()).thenReturn(null);

        userController.callDoDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(authenticationService, never()).deleteUser(anyString());
    }

    @Test
    @DisplayName("UserController DELETE: unexpected exception translates to 500")
    public void testUserDeleteUnexpectedException() throws Exception {
        when(request.getPathInfo()).thenReturn("/user@example.com");
        when(authenticationService.deleteUser("user@example.com"))
                .thenThrow(new RuntimeException("boom"));

        userController.callDoDelete(request, response);

        verify(authenticationService).deleteUser("user@example.com");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
