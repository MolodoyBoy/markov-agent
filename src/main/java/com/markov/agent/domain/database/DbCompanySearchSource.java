package com.markov.agent.domain.database;

import com.markov.agent.domain.value_object.DailyStock;
import com.markov.agent.domain.source.CompanySearchSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DbCompanySearchSource implements CompanySearchSource {

    private final JdbcTemplate jdbcTemplate;

    public DbCompanySearchSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DailyStock searchLatestDailyStockReturn(String query) {
        String sql = "SELECT company_id, stock_date, value FROM company c " +
            "inner join daily_stock_return ms on c.id = ms.company_id " +
            "WHERE (company_name = ? or ticker = ?) " +
            "ORDER BY ms.company_id, ms.stock_date DESC " +
            "LIMIT 1;";

        RowMapper<DailyStock> rowMapper = (rs, rowNum) -> new DailyStock(
            rs.getInt("company_id"),
            rs.getBigDecimal("value"),
            rs.getDate("stock_date").toLocalDate()
        );

        List<DailyStock> dailyStockReturns = jdbcTemplate.query(sql, rowMapper, query, query);
        if (dailyStockReturns.isEmpty()) {
            return null;
        }

        return dailyStockReturns.get(0);
    }
}
