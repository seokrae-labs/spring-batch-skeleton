CREATE TABLE IF NOT EXISTS sample_record (
    target_date DATE         NOT NULL,
    item_key    VARCHAR(100) NOT NULL,
    amount      BIGINT       NOT NULL,
    PRIMARY KEY (target_date, item_key)
);
