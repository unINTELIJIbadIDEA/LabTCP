package com.unINTELIJIbadIDEA.server.services;

import com.unINTELIJIbadIDEA.server.dao.ResultDAO;
import java.sql.SQLException;
import java.util.List;

public class ResultService {
    private final ResultDAO dao;

    public ResultService() {
        this.dao = new ResultDAO();
    }

    public void save(String username, List<String> answers) throws SQLException {
        try {
            dao.connect();
            dao.save(username, answers);
        } finally {
            dao.close();
        }
    }
}