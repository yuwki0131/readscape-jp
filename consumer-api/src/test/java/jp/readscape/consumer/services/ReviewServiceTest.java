package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.reviews.model.Review;
import jp.readscape.consumer.domain.reviews.repository.ReviewRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.reviews.BookReviewsResponse;
import jp.readscape.consumer.dto.reviews.PostReviewRequest;
import jp.readscape.consumer.dto.reviews.ReviewResponse;
import jp.readscape.consumer.dto.reviews.ReviewSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookService bookService;

    @InjectMocks
    private ReviewService reviewService;

    private Book testBook;
    private User testUser;
    private Review testReview;
    private PostReviewRequest postReviewRequest;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("9781234567890")
                .price(1500)
                .averageRating(BigDecimal.valueOf(4.2))
                .reviewCount(10)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        testReview = Review.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .rating(5)
                .title("Great book!")
                .comment("I really enjoyed this book.")
                .isVerifiedPurchase(true)
                .helpfulCount(3)
                .createdAt(LocalDateTime.now())
                .build();

        postReviewRequest = PostReviewRequest.builder()
                .rating(5)
                .title("Great book!")
                .comment("I really enjoyed this book.")
                .build();
    }

    @Test
    void getBookReviews_WithValidBookId_ShouldReturnBookReviewsResponse() {
        // Given
        Page<Review> reviewPage = new PageImpl<>(Arrays.asList(testReview), PageRequest.of(0, 10), 1);
        Object[] statistics = {4.2, 10, 3, 4, 2, 1, 0}; // avg, total, 5star, 4star, 3star, 2star, 1star

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(reviewPage);
        when(reviewRepository.getBookRatingStatistics(1L)).thenReturn(statistics);

        // When
        BookReviewsResponse response = reviewService.getBookReviews(1L, 0, 10, "newest");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBookId()).isEqualTo(1L);
        assertThat(response.getBookTitle()).isEqualTo("Test Book");
        assertThat(response.getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.2));
        assertThat(response.getTotalReviews()).isEqualTo(10);
        assertThat(response.getReviews()).hasSize(1);
        assertThat(response.getCurrentPage()).isZero();
        assertThat(response.getTotalPages()).isEqualTo(1);

        verify(bookRepository).findById(1L);
        verify(reviewRepository).getBookRatingStatistics(1L);
    }

    @Test
    void getBookReviews_WithNonExistentBook_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.getBookReviews(1L, 0, 10, "newest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("書籍が見つかりません");
    }

    @Test
    void getBookReviews_WithHelpfulSort_ShouldReturnHelpfulReviews() {
        // Given
        List<Review> helpfulReviews = Arrays.asList(testReview);
        Object[] statistics = {4.2, 10, 3, 4, 2, 1, 0};

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findHelpfulReviewsByBookId(eq(1L), any(Pageable.class)))
                .thenReturn(helpfulReviews);
        when(reviewRepository.getBookRatingStatistics(1L)).thenReturn(statistics);

        // When
        BookReviewsResponse response = reviewService.getBookReviews(1L, 0, 10, "helpful");

        // Then
        assertThat(response.getReviews()).hasSize(1);
        verify(reviewRepository).findHelpfulReviewsByBookId(eq(1L), any(Pageable.class));
    }

    @Test
    void getBookReviews_WithPositiveSort_ShouldReturnPositiveReviews() {
        // Given
        List<Review> positiveReviews = Arrays.asList(testReview);
        Object[] statistics = {4.2, 10, 3, 4, 2, 1, 0};

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findPositiveReviewsByBookId(eq(1L), any(Pageable.class)))
                .thenReturn(positiveReviews);
        when(reviewRepository.getBookRatingStatistics(1L)).thenReturn(statistics);

        // When
        BookReviewsResponse response = reviewService.getBookReviews(1L, 0, 10, "positive");

        // Then
        assertThat(response.getReviews()).hasSize(1);
        verify(reviewRepository).findPositiveReviewsByBookId(eq(1L), any(Pageable.class));
    }

    @Test
    void getBookReviews_WithNegativeSort_ShouldReturnNegativeReviews() {
        // Given
        List<Review> negativeReviews = Arrays.asList(testReview);
        Object[] statistics = {4.2, 10, 3, 4, 2, 1, 0};

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findNegativeReviewsByBookId(eq(1L), any(Pageable.class)))
                .thenReturn(negativeReviews);
        when(reviewRepository.getBookRatingStatistics(1L)).thenReturn(statistics);

        // When
        BookReviewsResponse response = reviewService.getBookReviews(1L, 0, 10, "negative");

        // Then
        assertThat(response.getReviews()).hasSize(1);
        verify(reviewRepository).findNegativeReviewsByBookId(eq(1L), any(Pageable.class));
    }

    @Test
    void getBookReviews_WithVerifiedSort_ShouldReturnVerifiedReviews() {
        // Given
        Page<Review> verifiedReviews = new PageImpl<>(Arrays.asList(testReview), PageRequest.of(0, 10), 1);
        Object[] statistics = {4.2, 10, 3, 4, 2, 1, 0};

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findByBookIdAndIsVerifiedPurchaseTrueOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(verifiedReviews);
        when(reviewRepository.getBookRatingStatistics(1L)).thenReturn(statistics);

        // When
        BookReviewsResponse response = reviewService.getBookReviews(1L, 0, 10, "verified");

        // Then
        assertThat(response.getReviews()).hasSize(1);
        verify(reviewRepository).findByBookIdAndIsVerifiedPurchaseTrueOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void getBookReviews_WithNullStatistics_ShouldHandleGracefully() {
        // Given
        Page<Review> reviewPage = new PageImpl<>(Arrays.asList(testReview), PageRequest.of(0, 10), 1);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(reviewPage);
        when(reviewRepository.getBookRatingStatistics(1L)).thenReturn(null);

        // When
        BookReviewsResponse response = reviewService.getBookReviews(1L, 0, 10, "newest");

        // Then
        assertThat(response.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalReviews()).isZero();
        assertThat(response.getRatingDistribution()).isNotNull();
    }

    @Test
    void postReview_WithValidRequest_ShouldCreateReview() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(false);
        when(reviewRepository.hasUserPurchasedBook(1L, 1L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        doNothing().when(bookService).updateBookRating(1L);

        // When
        ReviewResponse response = reviewService.postReview(1L, 1L, postReviewRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getTitle()).isEqualTo("Great book!");

        verify(reviewRepository).save(any(Review.class));
        verify(bookService).updateBookRating(1L);
    }

    @Test
    void postReview_WithNonExistentBook_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.postReview(1L, 1L, postReviewRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("書籍が見つかりません");
    }

    @Test
    void postReview_WithDuplicateReview_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> reviewService.postReview(1L, 1L, postReviewRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("すでにこの書籍にレビューを投稿済みです");
    }

    @Test
    void postReview_WithNoPurchaseHistory_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(false);
        when(reviewRepository.hasUserPurchasedBook(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> reviewService.postReview(1L, 1L, postReviewRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("購入履歴のある書籍のみレビュー可能です");
    }

    @Test
    void deleteReview_WithValidReview_ShouldDeleteSuccessfully() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).delete(testReview);
        doNothing().when(bookService).updateBookRating(1L);

        // When
        reviewService.deleteReview(1L, 1L);

        // Then
        verify(reviewRepository).delete(testReview);
        verify(bookService).updateBookRating(1L);
    }

    @Test
    void deleteReview_WithNonExistentReview_ShouldThrowException() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.deleteReview(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("レビューが見つかりません");
    }

    @Test
    void deleteReview_WithDifferentUser_ShouldThrowException() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // When & Then - trying to delete with different user ID
        assertThatThrownBy(() -> reviewService.deleteReview(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("他人のレビューは削除できません");
    }

    @Test
    void updateReview_WithValidRequest_ShouldUpdateSuccessfully() {
        // Given
        PostReviewRequest updateRequest = PostReviewRequest.builder()
                .rating(4)
                .title("Updated title")
                .comment("Updated comment")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        doNothing().when(bookService).updateBookRating(1L);

        // When
        ReviewResponse response = reviewService.updateReview(1L, 1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(testReview.getRating()).isEqualTo(4);
        assertThat(testReview.getTitle()).isEqualTo("Updated title");
        assertThat(testReview.getComment()).isEqualTo("Updated comment");

        verify(reviewRepository).save(testReview);
        verify(bookService).updateBookRating(1L);
    }

    @Test
    void updateReview_WithNonExistentReview_ShouldThrowException() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.updateReview(1L, 1L, postReviewRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("レビューが見つかりません");
    }

    @Test
    void updateReview_WithDifferentUser_ShouldThrowException() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // When & Then - trying to update with different user ID
        assertThatThrownBy(() -> reviewService.updateReview(1L, 2L, postReviewRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("他人のレビューは更新できません");
    }

    @Test
    void markReviewAsHelpful_WithValidReview_ShouldIncreaseHelpfulCount() {
        // Given
        Integer originalCount = testReview.getHelpfulCount();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        // When
        reviewService.markReviewAsHelpful(1L);

        // Then
        assertThat(testReview.getHelpfulCount()).isEqualTo(originalCount + 1);
        verify(reviewRepository).save(testReview);
    }

    @Test
    void markReviewAsHelpful_WithNonExistentReview_ShouldThrowException() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.markReviewAsHelpful(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("レビューが見つかりません");
    }

    @Test
    void getUserReviews_WithValidUser_ShouldReturnReviewList() {
        // Given
        List<Review> userReviews = Arrays.asList(testReview);
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(userReviews);

        // When
        List<ReviewSummary> result = reviewService.getUserReviews(1L);

        // Then
        assertThat(result).hasSize(1);
        verify(reviewRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getUserReviews_WithNoReviews_ShouldReturnEmptyList() {
        // Given
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        // When
        List<ReviewSummary> result = reviewService.getUserReviews(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void searchReviews_WithValidKeyword_ShouldReturnSearchResults() {
        // Given
        Page<Review> searchResults = new PageImpl<>(Arrays.asList(testReview), PageRequest.of(0, 10), 1);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.searchReviewsByBookIdAndKeyword(eq(1L), eq("great"), any(Pageable.class)))
                .thenReturn(searchResults);

        // When
        BookReviewsResponse response = reviewService.searchReviews(1L, "great", 0, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBookId()).isEqualTo(1L);
        assertThat(response.getReviews()).hasSize(1);
        verify(reviewRepository).searchReviewsByBookIdAndKeyword(eq(1L), eq("great"), any(Pageable.class));
    }

    @Test
    void searchReviews_WithNonExistentBook_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.searchReviews(1L, "keyword", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("書籍が見つかりません");
    }

    @Test
    void searchReviews_WithNoResults_ShouldReturnEmptyResults() {
        // Given
        Page<Review> emptyResults = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.searchReviewsByBookIdAndKeyword(eq(1L), eq("nonexistent"), any(Pageable.class)))
                .thenReturn(emptyResults);

        // When
        BookReviewsResponse response = reviewService.searchReviews(1L, "nonexistent", 0, 10);

        // Then
        assertThat(response.getReviews()).isEmpty();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    void getBookReviews_WithEmptyStatisticsArray_ShouldHandleGracefully() {
        // Given
        Page<Review> reviewPage = new PageImpl<>(Arrays.asList(testReview), PageRequest.of(0, 10), 1);
        Object[] emptyStatistics = {null, null, null, null, null, null, null};

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(reviewPage);
        when(reviewRepository.getBookRatingStatistics(1L)).thenReturn(emptyStatistics);

        // When
        BookReviewsResponse response = reviewService.getBookReviews(1L, 0, 10, "newest");

        // Then
        assertThat(response.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalReviews()).isZero();
        assertThat(response.getRatingDistribution()).isNotNull();
    }
}