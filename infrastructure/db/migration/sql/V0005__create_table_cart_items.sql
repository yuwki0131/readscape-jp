
CREATE TABLE readscape.cart_items (
id SERIAL PRIMARY KEY,
cart_id INTEGER NOT NULL REFERENCES readscape.carts(id),
book_id INTEGER NOT NULL REFERENCES readscape.books(id),
quantity INTEGER NOT NULL,
UNIQUE(cart_id, book_id)
);

