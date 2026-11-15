package com.markov.agent.data_loader.database;

import com.markov.agent.data_loader.source.DailyStockReturnSource;
import com.markov.agent.domain.value_object.DailyStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

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

    @Override
    public Map<Integer, List<DailyStock>> getDailyStockReturns(LocalDateTime from) {
        String sql = "SELECT company_id, stock_date, value FROM daily_stock_return "
            + "WHERE stock_date >= ? ORDER BY company_id, stock_date";

        List<DailyStock> returns = jdbcTemplate.query(sql, (rs, rowNum) -> new DailyStock(
            rs.getInt("company_id"),
            rs.getBigDecimal("value"),
            rs.getDate("stock_date").toLocalDate()
        ), Timestamp.valueOf(from));

        return returns.stream().collect(groupingBy(DailyStock::companyId));
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
