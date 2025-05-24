package com.unINTELIJIbadIDEA.server.dao;

import com.unINTELIJIbadIDEA.utils.Config;

import java.sql.*;

public class ScoreDAO {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private Connection connection;

    public ScoreDAO() {
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

    public boolean save(String username, int score, int total) throws SQLException {
        String sql = "INSERT INTO quiz_scores (username, score, total_questions, recorded_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, score);
            stmt.setInt(3, total);
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            return stmt.executeUpdate() > 0;
        }
    }
}