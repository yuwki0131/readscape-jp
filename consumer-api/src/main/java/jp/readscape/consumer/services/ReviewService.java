package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.reviews.model.Review;
import jp.readscape.consumer.domain.reviews.repository.ReviewRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.reviews.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    /**
     * 書籍のレビュー一覧を取得
     */
    public BookReviewsResponse getBookReviews(Long bookId, Integer page, Integer size, String sortBy) {
        log.debug("Getting reviews for book: {}, page: {}, size: {}, sortBy: {}", bookId, page, size, sortBy);

        // 書籍の存在確認
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: " + bookId));

        // ページング設定
        Pageable pageable = PageRequest.of(page, size);
        
        // ソート条件に応じてレビューを取得
        Page<Review> reviewPage = getReviewsBySortType(bookId, sortBy, pageable);

        // レビューをDTOに変換
        List<ReviewResponse> reviews = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());

        // 評価統計を取得
        Object[] statistics = reviewRepository.getBookRatingStatistics(bookId);
        BigDecimal averageRating = BigDecimal.ZERO;
        Integer totalReviews = 0;
        BookReviewsResponse.RatingDistribution distribution = BookReviewsResponse.RatingDistribution.builder().build();

        if (statistics != null && statistics[0] != null) {
            averageRating = BigDecimal.valueOf((Double) statistics[0]).setScale(1, RoundingMode.HALF_UP);
            totalReviews = ((Number) statistics[1]).intValue();
            distribution = BookReviewsResponse.RatingDistribution.builder()
                    .rating5Count(((Number) statistics[2]).intValue())
                    .rating4Count(((Number) statistics[3]).intValue())
                    .rating3Count(((Number) statistics[4]).intValue())
                    .rating2Count(((Number) statistics[5]).intValue())
                    .rating1Count(((Number) statistics[6]).intValue())
                    .build();
        }

        return BookReviewsResponse.builder()
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .ratingDistribution(distribution)
                .reviews(reviews)
                .currentPage(reviewPage.getNumber())
                .totalPages(reviewPage.getTotalPages())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                .build();
    }

    /**
     * レビューを投稿
     */
    @Transactional
    public ReviewResponse postReview(Long bookId, Long userId, PostReviewRequest request) {
        log.debug("Posting review for book: {} by user: {}", bookId, userId);

        // 書籍の存在確認
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: " + bookId));

        // 重複レビューチェック
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new IllegalStateException("すでにこの書籍にレビューを投稿済みです");
        }

        // 購入履歴チェック（購入者のみレビュー可能）
        boolean hasPurchased = reviewRepository.hasUserPurchasedBook(userId, bookId);
        if (!hasPurchased) {
            throw new IllegalStateException("購入履歴のある書籍のみレビュー可能です");
        }

        // レビューを作成
        Review review = Review.builder()
                .book(book)
                .user(User.builder().id(userId).build()) // プロキシオブジェクト
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .isVerifiedPurchase(true) // 購入確認済み
                .build();

        Review savedReview = reviewRepository.save(review);

        // 書籍の評価情報を更新
        updateBookRating(bookId);

        log.info("Review posted successfully: {} for book: {} by user: {}", savedReview.getId(), bookId, userId);
        return ReviewResponse.from(savedReview);
    }

    /**
     * レビューを削除
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        log.debug("Deleting review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが見つかりません: " + reviewId));

        // 投稿者本人のみ削除可能
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("他人のレビューは削除できません");
        }

        Long bookId = review.getBook().getId();
        reviewRepository.delete(review);

        // 書籍の評価情報を更新
        updateBookRating(bookId);

        log.info("Review deleted successfully: {} for book: {}", reviewId, bookId);
    }

    /**
     * レビューを更新
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, PostReviewRequest request) {
        log.debug("Updating review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが見つかりません: " + reviewId));

        // 投稿者本人のみ更新可能
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("他人のレビューは更新できません");
        }

        // レビューを更新
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());

        Review updatedReview = reviewRepository.save(review);

        // 書籍の評価情報を更新
        updateBookRating(review.getBook().getId());

        log.info("Review updated successfully: {} for book: {}", reviewId, review.getBook().getId());
        return ReviewResponse.from(updatedReview);
    }

    /**
     * レビューに「役立った」を追加
     */
    @Transactional
    public void markReviewAsHelpful(Long reviewId) {
        log.debug("Marking review as helpful: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが見つかりません: " + reviewId));

        review.increaseHelpfulCount();
        reviewRepository.save(review);

        log.debug("Review marked as helpful: {}, new count: {}", reviewId, review.getHelpfulCount());
    }

    /**
     * ユーザーのレビュー一覧を取得
     */
    public List<ReviewSummary> getUserReviews(Long userId) {
        log.debug("Getting reviews by user: {}", userId);

        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return reviews.stream()
                .map(ReviewSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * レビュー検索
     */
    public BookReviewsResponse searchReviews(Long bookId, String keyword, Integer page, Integer size) {
        log.debug("Searching reviews for book: {}, keyword: {}", bookId, keyword);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: " + bookId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.searchReviewsByBookIdAndKeyword(bookId, keyword, pageable);

        List<ReviewResponse> reviews = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());

        return BookReviewsResponse.builder()
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .reviews(reviews)
                .currentPage(reviewPage.getNumber())
                .totalPages(reviewPage.getTotalPages())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                .build();
    }

    // プライベートメソッド

    /**
     * ソート条件に応じてレビューを取得
     */
    private Page<Review> getReviewsBySortType(Long bookId, String sortBy, Pageable pageable) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "newest") {
            case "helpful" -> {
                List<Review> helpfulReviews = reviewRepository.findHelpfulReviewsByBookId(bookId, pageable);
                yield createPageFromList(helpfulReviews, pageable);
            }
            case "positive" -> {
                List<Review> positiveReviews = reviewRepository.findPositiveReviewsByBookId(bookId, pageable);
                yield createPageFromList(positiveReviews, pageable);
            }
            case "negative" -> {
                List<Review> negativeReviews = reviewRepository.findNegativeReviewsByBookId(bookId, pageable);
                yield createPageFromList(negativeReviews, pageable);
            }
            case "verified" -> reviewRepository.findByBookIdAndIsVerifiedPurchaseTrueOrderByCreatedAtDesc(bookId, pageable);
            default -> reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable);
        };
    }

    private Page<Review> createPageFromList(List<Review> reviews, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reviews.size());
        List<Review> subList = reviews.subList(start, end);
        return new PageImpl<>(subList, pageable, reviews.size());
    }

    /**
     * 書籍の評価情報を更新
     */
    private void updateBookRating(Long bookId) {
        log.debug("Updating book rating for book: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: " + bookId));

        // 平均評価を計算
        Double avgRating = reviewRepository.calculateAverageRatingByBookId(bookId);
        if (avgRating != null) {
            book.setAverageRating(BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));
        } else {
            book.setAverageRating(BigDecimal.ZERO);
        }

        // レビュー数を更新
        Long reviewCount = reviewRepository.countByBookId(bookId);
        book.setReviewCount(reviewCount.intValue());

        bookRepository.save(book);
        log.debug("Book rating updated: {} (avg: {}, count: {})", bookId, book.getAverageRating(), book.getReviewCount());
    }
}