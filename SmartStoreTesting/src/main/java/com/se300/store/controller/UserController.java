package com.se300.store.controller;

import com.se300.store.model.User;
import com.se300.store.service.AuthenticationService;
import com.se300.store.servlet.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;

/**
 * REST API controller for User operations
 * Implements full CRUD operations.
 *
 * Endpoints:
 * GET /api/v1/users -> list all users
 * GET /api/v1/users/{email} -> get single user
 * POST /api/v1/users -> create user
 * PUT /api/v1/users/{email} -> update user
 * DELETE /api/v1/users/{email} -> delete user
 */
public class UserController extends BaseServlet {

    private final AuthenticationService authenticationService;

    public UserController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Handle GET requests.
     * - GET /api/v1/users -> all users
     * - GET /api/v1/users/{email} -> single user
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String email = extractResourceId(request);

            if (email == null || email.isBlank()) {
                Collection<User> users = authenticationService.getAllUsers();
                sendJsonResponse(response, users, HttpServletResponse.SC_OK);
            } else {
                User user = authenticationService.getUserByEmail(email);
                if (user == null) {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
                            "User not found: " + email);
                } else {
                    sendJsonResponse(response, user, HttpServletResponse.SC_OK);
                }
            }
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle POST requests - Create new user
     * POST /api/v1/users?email=xxx&password=xxx&name=xxx
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String name = request.getParameter("name");

        if (email == null || password == null || name == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required parameters: email, password, name");
            return;
        }

        try {
            if (authenticationService.userExists(email)) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT,
                        "User already exists: " + email);
                return;
            }

            User user = authenticationService.registerUser(email, password, name);
            sendJsonResponse(response, user, HttpServletResponse.SC_CREATED);
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle PUT requests - Update user information
     * PUT /api/v1/users/{email}?password=xxx&name=xxx
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = extractResourceId(request);
        if (email == null || email.isBlank()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Email is required in path: /api/v1/users/{email}");
            return;
        }

        String password = request.getParameter("password");
        String name = request.getParameter("name");

        if (password == null && name == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "At least one of password or name must be provided");
            return;
        }

        try {
            User updated = authenticationService.updateUser(email, password, name);
            if (updated == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        "User not found: " + email);
            } else {
                sendJsonResponse(response, updated, HttpServletResponse.SC_OK);
            }
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle DELETE requests - Delete user
     * DELETE /api/v1/users/{email}
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = extractResourceId(request);
        if (email == null || email.isBlank()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Email is required in path: /api/v1/users/{email}");
            return;
        }

        try {
            boolean deleted = authenticationService.deleteUser(email);
            if (!deleted) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        "User not found: " + email);
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }
}
