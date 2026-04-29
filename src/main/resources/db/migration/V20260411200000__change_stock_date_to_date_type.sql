ALTER TABLE daily_stock ALTER COLUMN stock_date TYPE date USING stock_date::date;
ALTER TABLE daily_stock_return ALTER COLUMN stock_date TYPE date USING stock_date::date;

