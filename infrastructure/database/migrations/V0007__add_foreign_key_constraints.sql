-- 外部キー制約の追加

-- books.category_id -> categories.id
ALTER TABLE readscape.books 
ADD CONSTRAINT fk_books_category_id 
FOREIGN KEY (category_id) REFERENCES readscape.categories(id);