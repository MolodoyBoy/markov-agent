package com.markov.agent.data_loader.database;

import com.markov.agent.data_loader.source.LockSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DbLockSource implements LockSource {

    private final JdbcTemplate jdbcTemplate;

    public DbLockSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void acquireLock(int lockKey) {
        jdbcTemplate.execute("SELECT PG_ADVISORY_XACT_LOCK(" + lockKey + ")");
    }
}