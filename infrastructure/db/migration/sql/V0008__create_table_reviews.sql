
CREATE TABLE readscape.reviews (
id SERIAL PRIMARY KEY,
book_id INTEGER NOT NULL REFERENCES readscape.books(id),
user_id INTEGER NOT NULL REFERENCES readscape.users(id),
rating INTEGER CHECK (rating BETWEEN 1 AND 5),
comment TEXT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

