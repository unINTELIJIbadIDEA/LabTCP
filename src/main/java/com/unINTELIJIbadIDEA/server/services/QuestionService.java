package com.unINTELIJIbadIDEA.server.services;

import com.unINTELIJIbadIDEA.server.dao.QuestionDAO;
import com.unINTELIJIbadIDEA.server.model.Question;

import java.sql.SQLException;
import java.util.List;

public class QuestionService {
    private final QuestionDAO dao;

    public QuestionService() {
        this.dao = new QuestionDAO();
    }

    public List<Question> getAll() throws SQLException {
        try {
            dao.connect();
            return dao.getAll();
        } finally {
            dao.close();
        }
    }
}