-- Seed data for local development and Docker Compose smoke tests.
-- Password for both users: "password" (bcrypt, strength 10).

INSERT INTO users (email, password_hash, full_name, role)
VALUES
    (
        'admin@example.com',
        '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3RJW/ZIO/Atn/kY5.rRLL2',
        'System Admin',
        'ADMIN'
    ),
    (
        'customer@example.com',
        '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3RJW/ZIO/Atn/kY5.rRLL2',
        'Jane Customer',
        'CUSTOMER'
    );

INSERT INTO products (name, description, price, active)
VALUES
    ('Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, TRUE),
    ('Mechanical Keyboard', 'Tenkeyless mechanical keyboard, Cherry MX switches', 89.99, TRUE),
    ('USB-C Hub', '7-in-1 USB-C hub with HDMI and SD card reader', 49.99, TRUE),
    ('Monitor Stand', 'Adjustable aluminum monitor stand', 39.99, TRUE),
    ('Webcam HD', '1080p webcam with built-in microphone', 59.99, TRUE);

INSERT INTO inventory (product_id, quantity_available)
SELECT id, qty
FROM (VALUES
    (1, 100),
    (2, 50),
    (3, 75),
    (4, 30),
    (5, 1)
) AS seed(id, qty);
