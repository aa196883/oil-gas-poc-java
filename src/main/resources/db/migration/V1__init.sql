CREATE TABLE metadata (
    id BIGINT PRIMARY KEY,

    family TEXT,
    zone TEXT,
    logic TEXT,
    benchmark_id TEXT,
    reference TEXT,
    expected_unit TEXT,

    bloomberg_ticker TEXT,
    series_id TEXT,
    provider TEXT,
    symbol TEXT,

    instrument TEXT,
    exchange TEXT,
    currency TEXT,
    unit TEXT,
    price_field TEXT,

    roll_rule TEXT,
    qualification TEXT,
    notes TEXT,

    enabled INTEGER,

    start_date DATE,
    end_date DATE,

    last_status TEXT,
    last_error TEXT
);

CREATE TABLE prices (
    metadata_id BIGINT NOT NULL,
    date DATE NOT NULL,
    price NUMERIC(24, 12) NOT NULL,

    PRIMARY KEY (metadata_id, date),

    CONSTRAINT fk_prices_metadata
        FOREIGN KEY (metadata_id)
        REFERENCES metadata(id)
);