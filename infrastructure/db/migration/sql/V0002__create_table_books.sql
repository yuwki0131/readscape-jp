
CREATE TABLE readscape.books (
id SERIAL PRIMARY KEY,
title TEXT NOT NULL,
author TEXT NOT NULL,
price INTEGER NOT NULL,
category TEXT,
rating FLOAT
);

