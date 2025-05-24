package com.unINTELIJIbadIDEA.server.dao;

import com.unINTELIJIbadIDEA.utils.Config;

import java.sql.*;
import java.util.List;

public class ResultDAO {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private Connection connection;

    public ResultDAO() {
        this.dbUrl = Config.getProperty("db.URL");
        this.dbUser = Config.getProperty("db.Username");
        this.dbPassword = Config.getProperty("db.Password");
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public boolean save(String username, List<String> answers) throws SQLException {
        String sql = "INSERT INTO quiz_results (username, answers, answered_at) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, String.join(" ", answers));
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            return stmt.executeUpdate() > 0;
        }
    }
}