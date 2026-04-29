package com.markov.agent.data_loader.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markov.agent.data_loader.source.NewCompaniesQueueSource;
import com.markov.agent.data_loader.value_object.SaveCompanyCmd;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class DbNewCompaniesQueueSource implements NewCompaniesQueueSource {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public DbNewCompaniesQueueSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void delete(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "DELETE FROM new_companies_queue WHERE id IN (" + placeholders + ")";
        jdbcTemplate.update(sql, ids.toArray());
    }

    @Override
    public Map<Integer, SaveCompanyCmd> poll(int limit) {
        String sql = "SELECT id, payload FROM new_companies_queue ORDER BY id LIMIT ?";
        Map<Integer, SaveCompanyCmd> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            try {
                SaveCompanyCmd payload = objectMapper.readValue(rs.getString("payload"), SaveCompanyCmd.class);
                result.put(rs.getInt("id"), payload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize queue payload", e);
            }
        }, limit);
        return result;
    }

    @Override
    public void save(Collection<SaveCompanyCmd> companyCmds) {
        if (companyCmds.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO new_companies_queue (payload) VALUES ");
        Object[] params = new Object[companyCmds.size()];

        int i = 0;
        for (SaveCompanyCmd cmd : companyCmds) {
            if (i > 0) sql.append(", ");
            sql.append("(?::jsonb)");
            try {
                params[i] = objectMapper.writeValueAsString(cmd);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize queue payload", e);
            }
            i++;
        }

        jdbcTemplate.update(sql.toString(), params);
    }
}
