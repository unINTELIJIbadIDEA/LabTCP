package com.unINTELIJIbadIDEA.server.init;

import com.unINTELIJIbadIDEA.server.model.Question;
import com.unINTELIJIbadIDEA.utils.Config;

import java.sql.*;
import java.util.List;

public class DatabaseInitializer {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String dbName;

    public DatabaseInitializer() {
        this.dbUrl = Config.getProperty("db.URL");
        this.dbUser = Config.getProperty("db.Username");
        this.dbPassword = Config.getProperty("db.Password");

        this.dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
    }

    public void initializeDatabase() {
        String baseUrl = dbUrl.substring(0, dbUrl.lastIndexOf("/"));

        try (Connection conn = DriverManager.getConnection(baseUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");

        } catch (SQLException e) {
            System.out.println("Błąd podczas tworzenia bazy danych: " + e.getMessage());
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS questions (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    text TEXT NOT NULL,
                    option_a VARCHAR(255) NOT NULL,
                    option_b VARCHAR(255) NOT NULL,
                    option_c VARCHAR(255) NOT NULL,
                    option_d VARCHAR(255) NOT NULL,
                    correct_option CHAR(1) NOT NULL CHECK (correct_option IN ('A', 'B', 'C', 'D'))
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quiz_results (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(100) NOT NULL,
                    answers TEXT NOT NULL,
                    answered_at TIMESTAMP NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quiz_scores (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(100) NOT NULL,
                    score INT NOT NULL,
                    total_questions INT NOT NULL,
                    recorded_at TIMESTAMP NOT NULL
                )
            """);

        } catch (SQLException e) {
            System.err.println("Błąd podczas tworzenia tabel: " + e.getMessage());
        }

        loadInitialDataIfEmpty(Config.getProperty("questionsFile"));
    }

    private void loadInitialDataIfEmpty(String questionsFilePath) {
        List<Question> questions = QuestionsLoader.loadQuestions(questionsFilePath);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM questions");
            rs.next();
            int count = rs.getInt(1);

            if (count == 0) {
                String insertSql = """
                    INSERT INTO questions (text, option_a, option_b, option_c, option_d, correct_option)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    for (Question q : questions) {
                        pstmt.setString(1, q.getQuestionText());
                        pstmt.setString(2, q.getAnswerA());
                        pstmt.setString(3, q.getAnswerB());
                        pstmt.setString(4, q.getAnswerC());
                        pstmt.setString(5, q.getAnswerD());
                        pstmt.setString(6, q.getCorrectAnswer().toUpperCase());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                    System.out.println("Załadowano pytania początkowe.");
                }
            } else {
                System.out.println("Tabela `questions` nie jest pusta – pominięto ładowanie danych.");
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas ładowania danych początkowych: " + e.getMessage());
        }
    }
}
