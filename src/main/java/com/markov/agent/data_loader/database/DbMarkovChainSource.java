package com.markov.agent.data_loader.database;

import com.markov.agent.domain.value_object.MarkovChain;
import com.markov.agent.domain.value_object.MarkovChainPrediction;
import com.markov.agent.domain.value_object.MathIndex;
import com.markov.agent.domain.value_object.State;
import com.markov.agent.domain.value_object.DailyStock;
import com.markov.agent.domain.source.MarkovChainSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.markov.agent.domain.value_object.State.*;
import static com.markov.agent.domain.value_object.MathIndex.*;

@Repository
public class DbMarkovChainSource implements MarkovChainSource {

    private final JdbcTemplate jdbcTemplate;
    private final Map<MathIndex, String> sqlContext = Map.of(
        AVG, "SELECT ?, AVG(value) FROM daily_stock_return WHERE stock_date >= ?",
        STD_DEV, "SELECT ?, STDDEV(value) FROM daily_stock_return WHERE stock_date >= ?"
    );

    public DbMarkovChainSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MarkovChain getMarkovChain() {
        String sql = """
            SELECT from_state, to_state, probability
            FROM markov_chain
            ORDER BY from_state, to_state
            """;

        Map<State, List<MarkovChain.Cell>> cellsByState = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            State fromState = fromValue(rs.getInt("from_state"));
            State toState = fromValue(rs.getInt("to_state"));
            BigDecimal probability = rs.getBigDecimal("probability");

            cellsByState.computeIfAbsent(fromState, k -> new ArrayList<>())
                .add(new MarkovChain.Cell(toState, probability));
        });

        return new MarkovChain(cellsByState);
    }

    @Override
    public MarkovChainPrediction getMarkovChainPrediction(DailyStock dailyStock) {
        String classifySql = """
            SELECT CASE
                WHEN ? < s.avg_value - 0.35 * s.std_dev_value THEN 1
                WHEN ? > s.avg_value + 0.35 * s.std_dev_value THEN 3
                ELSE 2
            END AS state
            FROM (
                SELECT
                    MAX(CASE WHEN id = 1 THEN index_value END) AS avg_value,
                    MAX(CASE WHEN id = 2 THEN index_value END) AS std_dev_value
                FROM math_index
            ) s
            """;

        Integer classifiedState = jdbcTemplate.queryForObject(
            classifySql,
            Integer.class,
            dailyStock.closeValue(), dailyStock.closeValue()
        );

        String probabilitiesSql = """
            SELECT to_state, probability
            FROM markov_chain
            WHERE from_state = ?
            """;

        Map<State, BigDecimal> stateProbabilities = new HashMap<>();
        RowCallbackHandler rowCallbackHandler = rs -> {
            int toState = rs.getInt("to_state");
            BigDecimal probability = rs.getBigDecimal("probability");
            State state = fromValue(toState);
            stateProbabilities.put(state, probability);
        };

        jdbcTemplate.query(probabilitiesSql, rowCallbackHandler, classifiedState);

        return new MarkovChainPrediction(stateProbabilities);
    }

    @Override
    public void updateMathIndex(LocalDateTime from, MathIndex mathIndex) {
        String sql = sqlContext.get(mathIndex);
        if (sql == null) {
            throw new IllegalArgumentException("Unsupported math index: " + mathIndex);
        }

        String updateSql = "INSERT INTO math_index (" + sql + ") ON CONFLICT (id) DO UPDATE SET index_value = EXCLUDED.index_value";

        jdbcTemplate.update(updateSql, mathIndex.getIndex(), Timestamp.valueOf(from));
    }

    @Override
    public void updateMarkovChain(State from, State to, LocalDateTime fromDate) {
        String sql = """
           
            INSERT INTO markov_chain
            SELECT ?, ?, COALESCE((
                   WITH stats AS (
                        SELECT
                            MAX(CASE WHEN id = 1 THEN index_value END) AS avg_value,
                            MAX(CASE WHEN id = 2 THEN index_value END) AS std_dev_value
                        FROM math_index
                        ),
                       classified AS (
                            SELECT
                                msr.company_id,
                                msr.stock_date,
                            CASE
                            WHEN msr.value < s.avg_value - 0.35 * s.std_dev_value THEN 1
                                WHEN msr.value > s.avg_value + 0.35 * s.std_dev_value THEN 3
                            ELSE 2
                            END AS state
                    FROM daily_stock_return msr
                        CROSS JOIN stats s
                    WHERE msr.stock_date >= ?
                    ),
                    transitions AS (
                        SELECT
                            company_id,
                            stock_date,
                            state AS from_state,
                            LEAD(state) OVER ( PARTITION BY company_id ORDER BY stock_date) AS to_state,
                            LEAD(stock_date) OVER ( PARTITION BY company_id ORDER BY stock_date) AS next_stock_date
                         FROM classified
                    )
                    SELECT
                        COUNT(*) FILTER (
                        WHERE from_state = ? AND to_state = ? AND next_stock_date = stock_date + INTERVAL '1 day'
                    )::numeric
                    /
                    NULLIF(
                        COUNT(*) FILTER (
                            WHERE from_state = ? AND next_stock_date = stock_date + INTERVAL '1 day'
                        ),
                        0
                    )
                FROM transitions
                WHERE to_state IS NOT NULL
            ), 0) ON CONFLICT (from_state, to_state) DO UPDATE SET probability = EXCLUDED.probability
            """;

        jdbcTemplate.update(
            sql,
            from.getValue(), to.getValue(),
            Timestamp.valueOf(fromDate),
            from.getValue(), to.getValue(),
            from.getValue()
        );
    }
}
