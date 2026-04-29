package com.markov.agent.data_loader.database;

import com.markov.agent.data_loader.source.DailyStockReturnSource;
import com.markov.agent.domain.value_object.DailyStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public class DbDailyStockReturnSource implements DailyStockReturnSource {

    private final JdbcTemplate jdbcTemplate;

    public DbDailyStockReturnSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveDailyReturnStock(Map<Integer, List<DailyStock>> dailyStockReturns) {
        String sql = "INSERT INTO daily_stock_return (company_id, stock_date, value) VALUES (?, ?, ?) "
            + "ON CONFLICT (company_id, stock_date) DO UPDATE SET value = EXCLUDED.value";

        jdbcTemplate.batchUpdate(sql, map(dailyStockReturns));
    }

    private List<Object[]> map(Map<Integer, List<DailyStock>> dailyStockReturns) {
        return dailyStockReturns.values()
            .stream()
            .flatMap(Collection::stream)
            .map(stock -> new Object[]{
                stock.companyId(),
                stock.stockDate(),
                stock.closeValue()
            })
            .toList();
    }
}
