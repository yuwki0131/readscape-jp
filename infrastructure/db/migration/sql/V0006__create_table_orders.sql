
CREATE TABLE readscape.orders (
id SERIAL PRIMARY KEY,
user_id INTEGER NOT NULL REFERENCES readscape.users(id),
total_price INTEGER NOT NULL,
status TEXT NOT NULL,
address TEXT NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

