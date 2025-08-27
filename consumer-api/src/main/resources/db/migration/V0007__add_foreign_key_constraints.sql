-- 外部キー制約の追加

-- books.category_id -> categories.id
ALTER TABLE readscape.books 
ADD CONSTRAINT fk_books_category_id 
FOREIGN KEY (category_id) REFERENCES readscape.categories(id);

-- 既存のテーブルにもスキーマプレフィックスとBIGINT型への変更を適用するための制約追加は次のmigrationで実施