package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.reviews.repository.ReviewRepository;
import jp.readscape.consumer.dto.books.BookDetail;
import jp.readscape.consumer.dto.books.BookSummary;
import jp.readscape.consumer.dto.books.BooksResponse;
import jp.readscape.consumer.exceptions.BookNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final MeterRegistry meterRegistry;
    
    private final Counter bookSearchCounter;
    private final Counter bookDetailCounter;
    
    public BookService(BookRepository bookRepository, ReviewRepository reviewRepository, MeterRegistry meterRegistry) {
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.meterRegistry = meterRegistry;
        this.bookSearchCounter = Counter.builder("readscape.book.search")
                .description("Book search requests")
                .register(meterRegistry);
        this.bookDetailCounter = Counter.builder("readscape.book.detail")
                .description("Book detail requests")
                .register(meterRegistry);
    }

    /**
     * 書籍一覧取得
     */
    @Timed(value = "readscape.book.search.time", description = "Book search processing time")
    public BooksResponse findBooks(String category, String keyword, Integer page, Integer size, String sortBy) {
        bookSearchCounter.increment();
        log.debug("Finding books with category: {}, keyword: {}, page: {}, size: {}", 
                 category, keyword, page, size);

        // ソート設定
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Book> bookPage;

        // 検索条件による分岐
        if (category != null && !category.trim().isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            // カテゴリー + キーワード検索
            bookPage = bookRepository.findByCategoryAndKeyword(category.trim(), keyword.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            // カテゴリー検索のみ
            bookPage = bookRepository.findByCategoryContainingIgnoreCase(category.trim(), pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // キーワード検索のみ
            bookPage = bookRepository.findByTitleOrAuthorContaining(keyword.trim(), pageable);
        } else {
            // 全件検索
            bookPage = bookRepository.findAll(pageable);
        }

        // DTOに変換
        List<BookSummary> bookSummaries = bookPage.getContent().stream()
                .map(this::convertToBookSummary)
                .toList();

        return BooksResponse.builder()
                .books(bookSummaries)
                .currentPage(bookPage.getNumber())
                .totalPages(bookPage.getTotalPages())
                .totalElements(bookPage.getTotalElements())
                .size(bookPage.getSize())
                .hasNext(bookPage.hasNext())
                .hasPrevious(bookPage.hasPrevious())
                .build();
    }

    /**
     * 書籍詳細取得
     */
    @Timed(value = "readscape.book.detail.time", description = "Book detail processing time")
    public BookDetail findBookById(Long bookId) {
        bookDetailCounter.increment();
        log.debug("Finding book by id: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + bookId));

        return convertToBookDetail(book);
    }

    /**
     * ISBN による書籍検索
     */
    public BookDetail findBookByIsbn(String isbn) {
        log.debug("Finding book by ISBN: {}", isbn);

        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));

        return convertToBookDetail(book);
    }

    /**
     * カテゴリー一覧取得
     */
    public List<String> findAllCategories() {
        log.debug("Finding all categories");
        return bookRepository.findAllCategories();
    }

    /**
     * 人気書籍取得
     */
    public List<BookSummary> findPopularBooks(Integer limit) {
        log.debug("Finding popular books with limit: {}", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> popularBooks = bookRepository.findPopularBooks(pageable);

        return popularBooks.stream()
                .map(this::convertToBookSummary)
                .toList();
    }

    /**
     * 評価の高い書籍取得
     */
    public List<BookSummary> findTopRatedBooks(Integer limit) {
        log.debug("Finding top rated books with limit: {}", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> topRatedBooks = bookRepository.findTopRatedBooks(pageable);

        return topRatedBooks.stream()
                .map(this::convertToBookSummary)
                .toList();
    }

    /**
     * 在庫のある書籍検索
     */
    public BooksResponse findBooksInStock(Integer page, Integer size) {
        log.debug("Finding books in stock with page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("title"));
        Page<Book> bookPage = bookRepository.findByStockQuantityGreaterThan(0, pageable);

        List<BookSummary> bookSummaries = bookPage.getContent().stream()
                .map(this::convertToBookSummary)
                .toList();

        return BooksResponse.builder()
                .books(bookSummaries)
                .currentPage(bookPage.getNumber())
                .totalPages(bookPage.getTotalPages())
                .totalElements(bookPage.getTotalElements())
                .size(bookPage.getSize())
                .hasNext(bookPage.hasNext())
                .hasPrevious(bookPage.hasPrevious())
                .build();
    }

    // プライベートメソッド

    private Sort createSort(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by("createdAt").descending(); // デフォルト: 新着順
        }

        return switch (sortBy.toLowerCase()) {
            case "title" -> Sort.by("title").ascending();
            case "author" -> Sort.by("author").ascending();
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "rating" -> Sort.by("averageRating").descending();
            case "popularity" -> Sort.by("reviewCount").descending();
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };
    }

    private BookSummary convertToBookSummary(Book book) {
        return BookSummary.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .price(book.getPrice())
                .category(book.getCategory())
                .averageRating(book.getAverageRating())
                .reviewCount(book.getReviewCount())
                .imageUrl(book.getImageUrl())
                .inStock(book.isInStock())
                .build();
    }

    private BookDetail convertToBookDetail(Book book) {
        return BookDetail.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .price(book.getPrice())
                .description(book.getDescription())
                .category(book.getCategory())
                .stockQuantity(book.getStockQuantity())
                .averageRating(book.getAverageRating())
                .reviewCount(book.getReviewCount())
                .imageUrl(book.getImageUrl())
                .inStock(book.isInStock())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }

    /**
     * 書籍の評価情報を更新
     */
    @Transactional
    public void updateBookRating(Long bookId) {
        log.debug("Updating book rating for book: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + bookId));

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
        log.debug("Book rating updated: {} (avg: {}, count: {})", 
            bookId, book.getAverageRating(), book.getReviewCount());
    }

    /**
     * 全書籍の評価情報を更新（バッチ処理用）
     * パフォーマンス向上のためバッチで処理
     */
    @Transactional
    public void updateAllBookRatings() {
        log.info("Starting batch update of all book ratings");

        List<Book> books = bookRepository.findAll();
        List<Book> updatedBooks = new ArrayList<>();
        
        for (Book book : books) {
            try {
                // 平均評価を計算
                Double avgRating = reviewRepository.calculateAverageRatingByBookId(book.getId());
                if (avgRating != null) {
                    book.setAverageRating(BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));
                } else {
                    book.setAverageRating(BigDecimal.ZERO);
                }

                // レビュー数を更新
                Long reviewCount = reviewRepository.countByBookId(book.getId());
                book.setReviewCount(reviewCount.intValue());
                
                updatedBooks.add(book);
            } catch (Exception e) {
                log.error("Failed to prepare rating update for book {}: {}", book.getId(), e.getMessage());
            }
        }

        // バッチで一括保存
        if (!updatedBooks.isEmpty()) {
            bookRepository.saveAll(updatedBooks);
        }

        log.info("Completed batch update of all book ratings for {} books", updatedBooks.size());
    }
}