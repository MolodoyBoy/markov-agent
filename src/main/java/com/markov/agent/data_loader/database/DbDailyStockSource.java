package com.markov.agent.data_loader.database;

import com.markov.agent.data_loader.source.DailyStockSource;
import com.markov.agent.domain.value_object.DailyStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Repository
public class DbDailyStockSource implements DailyStockSource {

    private final JdbcTemplate jdbcTemplate;

    public DbDailyStockSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveDailyStock(Map<Integer, List<DailyStock>> dailyStocks) {
        if (dailyStocks.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO daily_stock (company_id, close_return, stock_date) VALUES (?, ?, ?) "
            + "ON CONFLICT (company_id, stock_date) DO UPDATE SET close_return = EXCLUDED.close_return";

        jdbcTemplate.batchUpdate(sql, map(dailyStocks));
    }

    @Override
    public Map<Integer, DailyStock> getDailyStock(Set<Integer> companyIds, LocalDate date) {
        if (companyIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", Collections.nCopies(companyIds.size(), "?"));
        String sql = "SELECT company_id, stock_date, close_return FROM daily_stock WHERE company_id IN (" + placeholders + ") AND stock_date = ?";

        List<Object> params = new ArrayList<>(companyIds);
        params.add(Date.valueOf(date));

        Map<Integer, DailyStock> result = new HashMap<>();
        RowCallbackHandler rowCallbackHandler = rs -> {
            int companyId = rs.getInt("company_id");
            result.put(companyId, new DailyStock(
                companyId,
                rs.getBigDecimal("close_return"),
                rs.getDate("stock_date").toLocalDate()
            ));
        };

        jdbcTemplate.query(sql, rowCallbackHandler, params.toArray());

        return result;
    }

    private List<Object[]> map(Map<Integer, List<DailyStock>> dailyStocks) {
        return dailyStocks.values()
            .stream()
            .flatMap(List::stream)
            .map(stock -> new Object[]{
                stock.companyId(),
                stock.closeValue(),
                stock.stockDate()
            })
            .toList();
    }
}
