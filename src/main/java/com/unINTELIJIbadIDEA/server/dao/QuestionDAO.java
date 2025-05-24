package com.unINTELIJIbadIDEA.server.dao;

import com.unINTELIJIbadIDEA.server.model.Question;
import com.unINTELIJIbadIDEA.utils.Config;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private Connection connection;

    public QuestionDAO() {
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

    public List<Question> getAll() throws SQLException {
        String sql = "SELECT * FROM questions";
        List<Question> questions = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Question q = new Question(
                        rs.getString("text"),
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d"),
                        rs.getString("correct_option")
                );
                questions.add(q);
            }
        }
        return questions;
    }
}
