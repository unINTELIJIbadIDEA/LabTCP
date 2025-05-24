package com.unINTELIJIbadIDEA.server.services;

import com.unINTELIJIbadIDEA.server.dao.ScoreDAO;
import java.sql.SQLException;

public class ScoreService {
    private final ScoreDAO dao;

    public ScoreService() {
        this.dao = new ScoreDAO();
    }

    public void save(String username, int score, int total) throws SQLException {
        try {
            dao.connect();
            dao.save(username, score, total);
        } finally {
            dao.close();
        }
    }
}