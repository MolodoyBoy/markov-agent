CREATE TABLE company(
    id SERIAL PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL,
    company_name VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX ON company (ticker);