CREATE TABLE new_companies_queue(
    id SERIAL PRIMARY KEY,
    payload JSONB NOT NULL
);

