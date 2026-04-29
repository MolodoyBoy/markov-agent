CREATE TABLE daily_stock (
    id BIGSERIAL PRIMARY KEY,
    company_id INT NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    stock_date timestamp NOT NULL,
    close_return NUMERIC(15) NOT NULL
);

CREATE UNIQUE INDEX ON daily_stock (company_id, stock_date);