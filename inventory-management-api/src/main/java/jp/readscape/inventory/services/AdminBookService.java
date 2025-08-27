package jp.readscape.inventory.services;

import jp.readscape.inventory.domain.books.model.Book;
import jp.readscape.inventory.domain.books.repository.BookRepository;
import jp.readscape.inventory.domain.inventory.model.StockHistory;
import jp.readscape.inventory.domain.inventory.repository.StockHistoryRepository;
import jp.readscape.inventory.domain.users.model.User;
import jp.readscape.inventory.dto.admin.*;
import jp.readscape.inventory.exceptions.BookNotFoundException;
import jp.readscape.inventory.exceptions.DuplicateIsbnException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookService {

    private final BookRepository bookRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final DtoMappingService dtoMappingService;

    /**
     * 管理者向け書籍一覧取得
     */
    public Page<AdminBookView> getBooks(int page, int size, String status) {
        log.debug("Getting admin books - page: {}, size: {}, status: {}", page, size, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        
        Page<Book> books;
        if (status != null && !status.isEmpty()) {
            try {
                Book.BookStatus bookStatus = Book.BookStatus.valueOf(status.toUpperCase());
                books = bookRepository.findByStatus(bookStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid book status: {}", status);
                books = bookRepository.findAll(pageable);
            }
        } else {
            books = bookRepository.findAll(pageable);
        }

        return books.map(dtoMappingService::mapToAdminBookView);
    }

    /**
     * 書籍作成
     */
    @Transactional
    public CreateBookResponse createBook(CreateBookRequest request, Long userId) {
        log.debug("Creating book: {}", request.getTitle());

        // ISBN重複チェック
        if (request.getIsbn() != null && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateIsbnException("ISBNが既に存在します: " + request.getIsbn());
        }

        // 書籍を作成
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .publisher(request.getPublisher())
                .publishedDate(request.getPublishedDate())
                .status(Book.BookStatus.ACTIVE)
                .build();

        Book savedBook = bookRepository.save(book);

        // 初期在庫履歴を記録
        if (request.getStockQuantity() > 0) {
            recordStockHistory(savedBook, userId, StockHistory.StockChangeType.INBOUND,
                    request.getStockQuantity(), 0, request.getStockQuantity(),
                    "初期在庫登録", null);
        }

        log.info("Book created successfully: {} (ID: {})", savedBook.getTitle(), savedBook.getId());
        return dtoMappingService.mapToCreateBookResponse(savedBook);
    }

    /**
     * 書籍更新
     */
    @Transactional
    public void updateBook(Long bookId, UpdateBookRequest request, Long userId) {
        log.debug("Updating book: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("書籍が見つかりません: " + bookId));

        // ISBN重複チェック（自分以外）
        if (request.hasIsbn() && !request.getIsbn().equals(book.getIsbn())) {
            if (bookRepository.existsByIsbn(request.getIsbn())) {
                throw new DuplicateIsbnException("ISBNが既に存在します: " + request.getIsbn());
            }
        }

        // フィールドを更新（nullでない場合のみ）
        if (request.hasTitle()) {
            book.setTitle(request.getTitle());
        }
        if (request.hasAuthor()) {
            book.setAuthor(request.getAuthor());
        }
        if (request.hasIsbn()) {
            book.setIsbn(request.getIsbn());
        }
        if (request.hasPrice()) {
            book.setPrice(request.getPrice());
        }
        if (request.hasLowStockThreshold()) {
            book.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.hasDescription()) {
            book.setDescription(request.getDescription());
        }
        if (request.hasImageUrl()) {
            book.setImageUrl(request.getImageUrl());
        }
        if (request.hasCategory()) {
            book.setCategory(request.getCategory());
        }
        if (request.hasPublisher()) {
            book.setPublisher(request.getPublisher());
        }
        if (request.hasPublishedDate()) {
            book.setPublishedDate(request.getPublishedDate());
        }
        if (request.hasStatus()) {
            book.setStatus(request.getStatus());
        }

        bookRepository.save(book);
        log.info("Book updated successfully: {} (ID: {})", book.getTitle(), book.getId());
    }

    /**
     * 書籍削除
     */
    @Transactional
    public void deleteBook(Long bookId, Long userId) {
        log.debug("Deleting book: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("書籍が見つかりません: " + bookId));

        // 在庫がある場合は削除前に在庫を0にする
        if (book.getStockQuantity() > 0) {
            Integer oldStock = book.getStockQuantity();
            book.setStockQuantity(0);
            
            recordStockHistory(book, userId, StockHistory.StockChangeType.ADJUSTMENT_DECREASE,
                    -oldStock, oldStock, 0, "書籍削除による在庫調整", null);
        }

        bookRepository.delete(book);
        log.info("Book deleted successfully: {} (ID: {})", book.getTitle(), book.getId());
    }

    /**
     * 書籍詳細取得
     */
    public AdminBookView getBookDetail(Long bookId) {
        log.debug("Getting book detail: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("書籍が見つかりません: " + bookId));

        return dtoMappingService.mapToAdminBookView(book);
    }

    /**
     * 低在庫書籍一覧取得
     */
    public List<AdminBookView> getLowStockBooks() {
        log.debug("Getting low stock books");

        List<Book> lowStockBooks = bookRepository.findLowStockBooks();
        return lowStockBooks.stream()
                .map(dtoMappingService::mapToAdminBookView)
                .collect(Collectors.toList());
    }

    /**
     * 在庫切れ書籍一覧取得
     */
    public List<AdminBookView> getOutOfStockBooks() {
        log.debug("Getting out of stock books");

        List<Book> outOfStockBooks = bookRepository.findOutOfStockBooks();
        return outOfStockBooks.stream()
                .map(dtoMappingService::mapToAdminBookView)
                .collect(Collectors.toList());
    }

    /**
     * カテゴリ一覧取得
     */
    public List<String> getCategories() {
        log.debug("Getting categories");
        return bookRepository.findDistinctCategories();
    }

    /**
     * キーワード検索
     */
    public Page<AdminBookView> searchBooks(String keyword, int page, int size) {
        log.debug("Searching books with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<Book> books = bookRepository.findByKeyword(keyword, pageable);
        
        return books.map(dtoMappingService::mapToAdminBookView);
    }

    // プライベートメソッド

    /**
     * 在庫履歴を記録
     */
    private void recordStockHistory(Book book, Long userId, StockHistory.StockChangeType type,
                                   Integer quantityChange, Integer quantityBefore, Integer quantityAfter,
                                   String reason, String referenceNumber) {
        StockHistory history = StockHistory.builder()
                .book(book)
                .user(User.builder().id(userId).build()) // プロキシオブジェクト
                .type(type)
                .quantityChange(quantityChange)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .reason(reason)
                .referenceNumber(referenceNumber)
                .build();

        stockHistoryRepository.save(history);
        log.debug("Stock history recorded: book={}, type={}, change={}", book.getId(), type, quantityChange);
    }
}