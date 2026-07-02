CREATE TABLE IF NOT EXISTS log_entries (
    id          BIGSERIAL PRIMARY KEY,
    timestamp   BIGINT       NOT NULL,
    ip          VARCHAR(20)  NOT NULL,
    method      VARCHAR(10)  NOT NULL,
    path        VARCHAR(100) NOT NULL,
    status      INT          NOT NULL,
    received_at BIGINT       NOT NULL
);