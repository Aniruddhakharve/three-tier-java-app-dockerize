package com.threetier.webapp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * REST API servlet for user management
 */
public class UserServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @Override
    public void init() throws ServletException {
        LOGGER.info("Initializing UserServlet...");

        // Initialize database with improved error handling
        boolean dbInitialized = DatabaseConnection.initializeDatabase();

        if (!dbInitialized) {
            LOGGER.warning("Database initialization failed or had permission issues");
            LOGGER.warning("UserServlet will continue but may have limited functionality");
        } else {
            LOGGER.info("Database initialized successfully");
        }

        LOGGER.info("UserServlet initialized successfully");
    }

    /**
     * GET /api/users/ - List all users
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LOGGER.info("GET request received for users list");
        setJsonResponse(response);

        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY id")) {

            while (rs.next()) {
                User user = new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("created_at")
                );
                users.add(user);
            }

            LOGGER.info("Retrieved " + users.size() + " users from database");

            writeJsonResponse(response, users);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while retrieving users", e);

            if (isPermissionError(e)) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Database permission error. Please contact the administrator."
                );
            } else if (isTableMissingError(e)) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database schema error. Please contact the administrator."
                );
            } else {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to retrieve users. Please try again later."
                );
            }
        }
    }

    /**
     * POST /api/users/ - Create new user
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LOGGER.info("POST request received for user creation");
        LOGGER.info("Content Type: " + request.getContentType());
        setJsonResponse(response);

        String name = request.getParameter("name");
        String email = request.getParameter("email");

        LOGGER.info(
            "Received parameters - name is null: "
                + (name == null)
                + ", email is null: "
                + (email == null)
        );

        if (name != null) {
            LOGGER.info("name is empty: " + name.trim().isEmpty());
        }

        if (email != null) {
            LOGGER.info("email is empty: " + email.trim().isEmpty());
        }

        // Validate input
        if (name == null || email == null || name.trim().isEmpty() || email.trim().isEmpty()) {
            LOGGER.warning("Validation failed: Name and email are required");
            sendErrorResponse(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Name and email are required"
            );
            return;
        }

        // Create and validate user object
        User newUser = new User(name.trim(), email.trim());

        if (!newUser.isValid()) {
            sendErrorResponse(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid email format"
            );
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO users (name, email) VALUES (?, ?) RETURNING id, created_at")) {

            stmt.setString(1, newUser.getName());
            stmt.setString(2, newUser.getEmail());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User createdUser = new User(
                    rs.getInt("id"),
                    newUser.getName(),
                    newUser.getEmail(),
                    rs.getString("created_at")
                );

                LOGGER.info("Created new user: " + createdUser);
                response.setStatus(HttpServletResponse.SC_CREATED);

                writeJsonResponse(response, createdUser);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database error while creating user", e);

            if (isDuplicateError(e)) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_CONFLICT,
                    "User with this email already exists"
                );
            } else if (isPermissionError(e)) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Database permission error. Please contact the administrator."
                );
            } else if (isTableMissingError(e)) {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database schema error. Please contact the administrator."
                );
            } else {
                sendErrorResponse(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to create user. Please try again later."
                );
            }
        }
    }

    /**
     * Configure JSON response headers.
     */
    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String allowedOrigin = System.getenv("ALLOWED_ORIGIN");

        if (allowedOrigin != null && !allowedOrigin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * Write an object as a JSON response.
     */
    @SuppressFBWarnings(
        value = "XSS_SERVLET",
        justification = "Gson safely serializes the object as JSON before writing the API response."
)
    private void writeJsonResponse(
            HttpServletResponse response,
            Object data) throws IOException {

        response.getWriter().write(GSON.toJson(data));
    }

    /**
     * Send error response in JSON format.
     */
    private void sendErrorResponse(
            HttpServletResponse response,
            int statusCode,
            String message) throws IOException {

        response.setStatus(statusCode);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("status", statusCode);

        writeJsonResponse(response, errorResponse);

        LOGGER.warning("Error response sent: " + message);
    }

    /**
     * Check whether the database error is caused by insufficient permissions.
     */
    private boolean isPermissionError(SQLException exception) {
        String message = exception.getMessage();

        return (message != null
                && (message.contains("permission") || message.contains("privilege")))
                || "42501".equals(exception.getSQLState());
    }

    /**
     * Check whether the database users table is missing.
     */
    private boolean isTableMissingError(SQLException exception) {
        String message = exception.getMessage();

        return message != null
                && message.contains("relation")
                && message.contains("does not exist");
    }

    /**
     * Check whether the database error is caused by a duplicate user.
     */
    private boolean isDuplicateError(SQLException exception) {
        String message = exception.getMessage();

        return message != null
                && (message.contains("duplicate key") || message.contains("unique"));
    }
}

