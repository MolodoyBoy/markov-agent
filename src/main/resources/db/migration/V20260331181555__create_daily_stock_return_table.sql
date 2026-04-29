CREATE TABLE daily_stock_return (
    id BIGSERIAL PRIMARY KEY,
    company_id INT NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    stock_date timestamp NOT NULL,
    value NUMERIC(15) NOT NULL
);

CREATE UNIQUE INDEX ON daily_stock_return (company_id, stock_date);