INSERT INTO products (id, name, description, price, available_quantity, version, created_at, updated_at)
VALUES
(1, 'Mechanical Keyboard', 'RGB mechanical keyboard with blue switches', 120.00, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 45.00, 30, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'USB-C Hub', 'Multi-port USB-C hub with HDMI and Ethernet', 65.00, 20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Laptop Stand', 'Adjustable aluminum laptop stand', 35.00, 25, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Noise Cancelling Headphones', 'Over-ear wireless headphones with active noise cancellation', 180.00, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE products ALTER COLUMN id RESTART WITH 6;

