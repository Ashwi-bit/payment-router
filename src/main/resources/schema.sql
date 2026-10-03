CREATE TABLE IF NOT EXISTS payments (
    idempotency_key  VARCHAR(255) PRIMARY KEY,
    amount           BIGINT NOT NULL,
    currency         VARCHAR(10) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    gateway          VARCHAR(50),
    attempts         INT DEFAULT 0,
    response_body    VARCHAR(2000),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS gateway_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway     VARCHAR(50) NOT NULL,
    success     INT NOT NULL,
    latency_ms  INT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gateway_events_gateway
    ON gateway_events(gateway, created_at DESC);