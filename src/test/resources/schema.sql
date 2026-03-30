CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(255) UNIQUE,
    user_id VARCHAR(255),
    order_status VARCHAR(50),
    pay_amount INT,
    total_amount INT,
    used_point INT,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(255),
    product_id VARCHAR(255),
    delivered_key VARCHAR(255),
    stock INT,
    unit_price INT
);

CREATE TABLE IF NOT EXISTS outbox (
    id BIGINT PRIMARY KEY,
    aggregateid VARCHAR(255),
    aggregatetype VARCHAR(50),
    type VARCHAR(50),
    payload LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);