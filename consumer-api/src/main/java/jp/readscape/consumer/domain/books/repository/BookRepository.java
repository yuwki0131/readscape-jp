package jp.readscape.consumer.domain.books.repository;

import jp.readscape.consumer.domain.books.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * カテゴリー別書籍検索
     */
    Page<Book> findByCategoryContainingIgnoreCase(String category, Pageable pageable);

    /**
     * キーワード検索（タイトルまたは著者名）
     */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Book> findByTitleOrAuthorContaining(@Param("keyword") String keyword, Pageable pageable);

    /**
     * カテゴリーとキーワードの複合検索
     */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.category) LIKE LOWER(CONCAT('%', :category, '%')) AND " +
           "( LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) )")
    Page<Book> findByCategoryAndKeyword(
        @Param("category") String category, 
        @Param("keyword") String keyword, 
        Pageable pageable
    );

    /**
     * 在庫のある書籍のみ検索
     */
    Page<Book> findByStockQuantityGreaterThan(Integer stockQuantity, Pageable pageable);

    /**
     * 価格範囲で検索
     */
    Page<Book> findByPriceBetween(Integer minPrice, Integer maxPrice, Pageable pageable);

    /**
     * 評価の高い書籍検索
     */
    @Query("SELECT b FROM Book b WHERE b.reviewCount > 0 ORDER BY b.averageRating DESC")
    List<Book> findTopRatedBooks(Pageable pageable);

    /**
     * 人気書籍検索（レビュー数順）
     */
    @Query("SELECT b FROM Book b WHERE b.reviewCount > 0 ORDER BY b.reviewCount DESC")
    List<Book> findPopularBooks(Pageable pageable);

    /**
     * カテゴリー一覧取得
     */
    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL ORDER BY b.category")
    List<String> findAllCategories();

    /**
     * ISBNによる検索
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * 在庫数による書籍検索
     */
    @Query("SELECT b FROM Book b WHERE b.stockQuantity > :minStock ORDER BY b.stockQuantity ASC")
    List<Book> findBooksWithMinimumStock(@Param("minStock") Integer minStock);
}