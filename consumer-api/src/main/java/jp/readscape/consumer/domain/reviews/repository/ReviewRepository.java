package jp.readscape.consumer.domain.reviews.repository;

import jp.readscape.consumer.domain.reviews.model.Review;
import jp.readscape.consumer.dto.reviews.BookReviewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 書籍IDでレビューを取得（ページング）
     */
    Page<Review> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    /**
     * 書籍IDでレビューを取得（リスト）
     */
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    /**
     * ユーザーと書籍でレビューを検索（重複チェック用）
     */
    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);

    /**
     * ユーザーと書籍でレビューの存在チェック
     */
    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    /**
     * ユーザーのレビュー一覧を取得
     */
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 書籍の平均評価を計算
     */
    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r WHERE r.book.id = :bookId")
    Double calculateAverageRatingByBookId(@Param("bookId") Long bookId);

    /**
     * 書籍のレビュー数を取得
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId")
    Long countByBookId(@Param("bookId") Long bookId);

    /**
     * 書籍の評価分布を取得
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.book.id = :bookId GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionByBookId(@Param("bookId") Long bookId);

    /**
     * 書籍の評価別レビュー数を取得
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId AND r.rating = :rating")
    Long countByBookIdAndRating(@Param("bookId") Long bookId, @Param("rating") Integer rating);

    /**
     * 購入確認済みレビューのみを取得
     */
    Page<Review> findByBookIdAndIsVerifiedPurchaseTrueOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    /**
     * 高評価レビューを取得（4星以上）
     */
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId AND r.rating >= 4 ORDER BY r.rating DESC, r.createdAt DESC")
    List<Review> findPositiveReviewsByBookId(@Param("bookId") Long bookId, Pageable pageable);

    /**
     * 低評価レビューを取得（2星以下）
     */
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId AND r.rating <= 2 ORDER BY r.rating ASC, r.createdAt DESC")
    List<Review> findNegativeReviewsByBookId(@Param("bookId") Long bookId, Pageable pageable);

    /**
     * 最新のレビューを取得
     */
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId ORDER BY r.createdAt DESC")
    List<Review> findLatestReviewsByBookId(@Param("bookId") Long bookId, Pageable pageable);

    /**
     * 役に立つレビューを取得（helpfulCount順）
     */
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    List<Review> findHelpfulReviewsByBookId(@Param("bookId") Long bookId, Pageable pageable);

    /**
     * コメント付きレビューのみを取得
     */
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId AND r.comment IS NOT NULL AND LENGTH(TRIM(r.comment)) > 0 ORDER BY r.createdAt DESC")
    Page<Review> findReviewsWithCommentsByBookId(@Param("bookId") Long bookId, Pageable pageable);

    /**
     * 特定のユーザーが特定の書籍を購入したことがあるかチェック
     */
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi JOIN oi.order o WHERE o.user.id = :userId AND oi.book.id = :bookId AND o.status != 'CANCELLED'")
    boolean hasUserPurchasedBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 書籍の評価統計を取得
     */
    @Query("""
        SELECT 
            AVG(CAST(r.rating AS double)) as avgRating,
            COUNT(r) as totalReviews,
            SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END) as rating5,
            SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END) as rating4,
            SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END) as rating3,
            SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END) as rating2,
            SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) as rating1
        FROM Review r 
        WHERE r.book.id = :bookId
    """)
    Object[] getBookRatingStatistics(@Param("bookId") Long bookId);

    /**
     * レビューのキーワード検索
     */
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId AND " +
           "(LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> searchReviewsByBookIdAndKeyword(@Param("bookId") Long bookId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 評価範囲でレビューを検索
     */
    @Query("SELECT r FROM Review r WHERE r.book.id = :bookId AND r.rating BETWEEN :minRating AND :maxRating ORDER BY r.createdAt DESC")
    Page<Review> findByBookIdAndRatingBetween(@Param("bookId") Long bookId, @Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating, Pageable pageable);
}