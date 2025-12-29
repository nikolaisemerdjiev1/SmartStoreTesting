package com.se300.store.controller;

import com.se300.store.model.Store;
import com.se300.store.model.StoreException;
import com.se300.store.service.StoreService;
import com.se300.store.servlet.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;

/**
 * REST API controller for Store operations
 * Implements full CRUD operations.
 *
 * Endpoints:
 * GET /api/v1/stores -> list all stores
 * GET /api/v1/stores/{storeId} -> get single store
 * POST /api/v1/stores -> create store
 * PUT /api/v1/stores/{storeId} -> update store
 * DELETE /api/v1/stores/{storeId} -> delete store
 */
public class StoreController extends BaseServlet {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * Handle GET requests.
     * - GET /api/v1/stores -> all stores
     * - GET /api/v1/stores/{storeId} -> single store
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String storeId = extractResourceId(request);

            if (storeId == null || storeId.isBlank()) {
                // List all stores
                Collection<Store> stores = storeService.getAllStores();
                sendJsonResponse(response, stores, HttpServletResponse.SC_OK);
            } else {
                // Get single store
                Store store = storeService.showStore(storeId, null); // token not used, pass null
                sendJsonResponse(response, store, HttpServletResponse.SC_OK);
            }
        } catch (StoreException e) {
            // Store not found or other business error
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            // Unexpected error
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle POST requests - Create new store
     * POST /api/v1/stores?storeId=xxx&description=xxx&address=xxx
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String storeId = request.getParameter("storeId");
        String description = request.getParameter("description");
        String address = request.getParameter("address");

        if (storeId == null || description == null || address == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required parameters: storeId, description, address");
            return;
        }

        try {
            // In StoreService, the second arg is called "name" but logically it’s the
            // description
            Store store = storeService.provisionStore(storeId, description, address, null);
            sendJsonResponse(response, store, HttpServletResponse.SC_CREATED);
        } catch (StoreException e) {
            // e.g., "Store Already Exists"
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle PUT requests - Update existing store
     * PUT /api/v1/stores/{storeId}?description=xxx&address=xxx
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String storeId = extractResourceId(request);
        if (storeId == null || storeId.isBlank()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Store ID is required in path: /api/v1/stores/{storeId}");
            return;
        }

        String description = request.getParameter("description");
        String address = request.getParameter("address");

        if (description == null && address == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "At least one of description or address must be provided");
            return;
        }

        try {
            Store updated = storeService.updateStore(storeId, description, address);
            sendJsonResponse(response, updated, HttpServletResponse.SC_OK);
        } catch (StoreException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle DELETE requests - Delete store
     * DELETE /api/v1/stores/{storeId}
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String storeId = extractResourceId(request);
        if (storeId == null || storeId.isBlank()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Store ID is required in path: /api/v1/stores/{storeId}");
            return;
        }

        try {
            storeService.deleteStore(storeId);
            // No body, just 204
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (StoreException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }
}
