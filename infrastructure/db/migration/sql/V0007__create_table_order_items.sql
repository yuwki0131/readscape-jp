
CREATE TABLE readscape.order_items (
id SERIAL PRIMARY KEY,
order_id INTEGER NOT NULL REFERENCES readscape.orders(id),
book_id INTEGER NOT NULL REFERENCES readscape.books(id),
price INTEGER NOT NULL,
quantity INTEGER NOT NULL
);

