package jp.readscape.inventory.domain.books.repository;

import jp.readscape.inventory.domain.books.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * ISBNで書籍を検索
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * タイトルで部分検索
     */
    @Query("SELECT b FROM Book b WHERE b.title ILIKE %:title%")
    List<Book> findByTitleContaining(@Param("title") String title);

    /**
     * ステータス別検索
     */
    Page<Book> findByStatus(Book.BookStatus status, Pageable pageable);

    /**
     * 低在庫商品検索
     */
    @Query("SELECT b FROM Book b WHERE b.stockQuantity <= b.lowStockThreshold AND b.status = 'ACTIVE'")
    List<Book> findLowStockBooks();

    /**
     * 在庫切れ商品検索
     */
    @Query("SELECT b FROM Book b WHERE b.stockQuantity <= 0 AND b.status = 'ACTIVE'")
    List<Book> findOutOfStockBooks();

    /**
     * カテゴリ別検索
     */
    Page<Book> findByCategory(String category, Pageable pageable);

    /**
     * 作成日範囲での検索
     */
    Page<Book> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * キーワード検索（タイトル、著者、カテゴリ）
     */
    @Query("SELECT b FROM Book b WHERE " +
           "b.title ILIKE %:keyword% OR " +
           "b.author ILIKE %:keyword% OR " +
           "b.category ILIKE %:keyword%")
    Page<Book> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 人気書籍（レビュー評価順）
     */
    @Query("SELECT b FROM Book b WHERE b.averageRating IS NOT NULL ORDER BY b.averageRating DESC, b.reviewCount DESC")
    List<Book> findPopularBooks(Pageable pageable);

    /**
     * 最近追加された書籍
     */
    List<Book> findTop10ByOrderByCreatedAtDesc();

    /**
     * ISBNの存在チェック
     */
    boolean existsByIsbn(String isbn);

    /**
     * 在庫統計
     */
    @Query("SELECT COUNT(b), SUM(b.stockQuantity), AVG(b.stockQuantity) FROM Book b WHERE b.status = 'ACTIVE'")
    Object[] getStockStatistics();

    /**
     * カテゴリ一覧
     */
    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL ORDER BY b.category")
    List<String> findDistinctCategories();

    /**
     * 売上上位書籍（注文アイテムとの結合）
     */
    @Query(value = "SELECT b.*, COALESCE(sales.total_quantity, 0) as sales_count " +
                   "FROM books b " +
                   "LEFT JOIN (SELECT oi.book_id, SUM(oi.quantity) as total_quantity " +
                           "FROM order_items oi " +
                           "JOIN orders o ON oi.order_id = o.id " +
                           "WHERE o.order_date >= :startDate " +
                           "AND o.status NOT IN ('CANCELLED') " +
                           "GROUP BY oi.book_id) sales ON b.id = sales.book_id " +
                   "ORDER BY sales_count DESC NULLS LAST", nativeQuery = true)
    List<Book> findTopSellingBooks(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}