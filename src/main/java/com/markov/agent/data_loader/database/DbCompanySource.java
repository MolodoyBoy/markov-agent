package com.markov.agent.data_loader.database;

import com.markov.agent.domain.source.CompanySearchSource;
import com.markov.agent.data_loader.source.CompanySource;
import com.markov.agent.data_loader.value_object.Company;
import com.markov.agent.domain.value_object.DailyStock;
import com.markov.agent.data_loader.value_object.SaveCompanyCmd;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class DbCompanySource implements CompanySource {

    private final JdbcTemplate jdbcTemplate;

    public DbCompanySource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Company> saveCompanies(Collection<SaveCompanyCmd> companies) {
        if (companies.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder(
            "INSERT INTO company (company_name, ticker) VALUES ");

        Object[] params = new Object[companies.size() * 2];

        int i = 0;
        for (SaveCompanyCmd cmd : companies) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?)");
            params[i * 2] = cmd.companyName();
            params[i * 2 + 1] = cmd.ticker();

            i += 1;
        }

        sql.append(" ON CONFLICT (ticker) DO UPDATE SET company_name = EXCLUDED.company_name RETURNING id, company_name, ticker");

        return jdbcTemplate.query(sql.toString(), companyRowMapper(), params);
    }

    @Override
    public Integer getFirstCompanyIdWithDailyStock() {
        String sql = "SELECT c.id FROM company c " +
            "INNER JOIN daily_stock ds ON c.id = ds.company_id " +
            "ORDER BY c.id LIMIT 1";
        ResultSetExtractor<Integer> resultSetExtractor = rs -> {
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        };

        return jdbcTemplate.query(sql, resultSetExtractor);
    }

    @Override
    public List<Company> getCompaniesWithDailyStock(Integer start, int batchSize) {
        String sql = "SELECT DISTINCT c.id, c.company_name, c.ticker FROM company c " +
            "INNER JOIN daily_stock ds ON c.id = ds.company_id " +
            "WHERE c.id >= ? ORDER BY c.id LIMIT ?";

        return jdbcTemplate.query(sql, companyRowMapper(), start, batchSize);
    }

    private RowMapper<Company> companyRowMapper() {
        return (rs, rowNum) -> new Company(
            rs.getInt("id"),
            rs.getString("company_name"),
            rs.getString("ticker")
        );
    }
}