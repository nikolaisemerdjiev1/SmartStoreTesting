package com.se300.store.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseServletAdditionalCoverageTest {

    /**
     * Simple concrete subclass so we can call the protected methods.
     */
    static class TestServlet extends BaseServlet {
        public String callReadRequestBody(HttpServletRequest request) throws IOException {
            return readRequestBody(request);
        }

        public void callSendJsonResponse(HttpServletResponse response, Object object) throws IOException {
            sendJsonResponse(response, object);
        }

        public String callExtractResourceId(HttpServletRequest request) {
            return extractResourceId(request);
        }
    }

    @Test
    @DisplayName("readRequestBody reads all lines into a single string")
    void readRequestBody_readsAllLines() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String body = "line1\nline2";
        BufferedReader reader = new BufferedReader(new StringReader(body));
        when(request.getReader()).thenReturn(reader);

        TestServlet servlet = new TestServlet();
        String result = servlet.callReadRequestBody(request);

        // Covers lines 28, 29, 31, 32, 34
        assertEquals("line1line2", result);
    }

    @Test
    @DisplayName("sendJsonResponse(response, object) uses 200 OK by default")
    void sendJsonResponse_defaultStatusIsOk() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        TestServlet servlet = new TestServlet();
        servlet.callSendJsonResponse(response, new DummyPojo("value"));

        printWriter.flush();

        // Covers lines 45, 46 (delegation to 3-arg version)
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertFalse(stringWriter.toString().isEmpty());
    }

    @Test
    @DisplayName("extractResourceId returns null for null or root path")
    void extractResourceId_nullOrSlash() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        TestServlet servlet = new TestServlet();

        when(request.getPathInfo()).thenReturn(null);
        String id1 = servlet.callExtractResourceId(request);
        assertNull(id1);

        when(request.getPathInfo()).thenReturn("/");
        String id2 = servlet.callExtractResourceId(request);
        assertNull(id2);

        // Covers the 'if' condition at line 95
    }

    @Test
    @DisplayName("extractResourceId returns first segment from path")
    void extractResourceId_firstSegmentReturned() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn("/S123/other");

        TestServlet servlet = new TestServlet();
        String id = servlet.callExtractResourceId(request);

        // Covers line 101 (non-null path branch)
        assertEquals("S123", id);
    }

    @Test
    @DisplayName("ErrorResponse getters return expected values (via reflection)")
    void errorResponse_getters() throws Exception {
        // Access the private static inner class BaseServlet.ErrorResponse
        Class<?> errorClass = Class.forName("com.se300.store.servlet.BaseServlet$ErrorResponse");

        Constructor<?> ctor = errorClass.getDeclaredConstructor(int.class, String.class);
        ctor.setAccessible(true);
        Object error = ctor.newInstance(400, "Bad request");

        Method getStatus = errorClass.getDeclaredMethod("getStatus");
        Method getMessage = errorClass.getDeclaredMethod("getMessage");
        Method getTimestamp = errorClass.getDeclaredMethod("getTimestamp");

        getStatus.setAccessible(true);
        getMessage.setAccessible(true);
        getTimestamp.setAccessible(true);

        int status = (Integer) getStatus.invoke(error);
        String message = (String) getMessage.invoke(error);
        long ts = (Long) getTimestamp.invoke(error);

        // Covers lines 119, 123, 127
        assertEquals(400, status);
        assertEquals("Bad request", message);
        assertTrue(ts > 0);
    }

    // Simple POJO to serialize in sendJsonResponse test
    static class DummyPojo {
        private final String field;

        DummyPojo(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }
}
