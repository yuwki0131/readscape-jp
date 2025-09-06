package jp.readscape.consumer.domain.books.repository;

import jp.readscape.consumer.domain.books.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    private Book techBook1;
    private Book techBook2;
    private Book novelBook1;
    private Book businessBook1;

    @BeforeEach
    void setUp() {
        // Create test books with various categories and attributes
        techBook1 = Book.builder()
                .title("Spring Boot in Action")
                .author("Craig Walls")
                .isbn("9781617292545")
                .description("Comprehensive guide to Spring Boot")
                .price(BigDecimal.valueOf(3500))
                .category("技術書")
                .publisher("Manning")
                .publishedDate(LocalDate.of(2023, 1, 15))
                .stockQuantity(10)
                .averageRating(BigDecimal.valueOf(4.5))
                .reviewCount(25)
                .build();

        techBook2 = Book.builder()
                .title("Java: The Complete Reference")
                .author("Herbert Schildt")
                .isbn("9781260463620")
                .description("Complete Java programming guide")
                .price(BigDecimal.valueOf(4200))
                .category("技術書")
                .publisher("McGraw-Hill")
                .publishedDate(LocalDate.of(2022, 8, 10))
                .stockQuantity(5)
                .averageRating(BigDecimal.valueOf(4.2))
                .reviewCount(15)
                .build();

        novelBook1 = Book.builder()
                .title("Norwegian Wood")
                .author("Haruki Murakami")
                .isbn("9784000000001")
                .description("A nostalgic story of loss and burgeoning sexuality")
                .price(BigDecimal.valueOf(1800))
                .category("小説")
                .publisher("Kodansha")
                .publishedDate(LocalDate.of(1987, 9, 4))
                .stockQuantity(0) // Out of stock
                .averageRating(BigDecimal.valueOf(4.8))
                .reviewCount(50)
                .build();

        businessBook1 = Book.builder()
                .title("The Lean Startup")
                .author("Eric Ries")
                .isbn("9780307887894")
                .description("How Today's Entrepreneurs Use Continuous Innovation")
                .price(BigDecimal.valueOf(2500))
                .category("ビジネス")
                .publisher("Crown Business")
                .publishedDate(LocalDate.of(2011, 9, 13))
                .stockQuantity(8)
                .averageRating(BigDecimal.valueOf(4.0))
                .reviewCount(30)
                .build();

        // Persist test data
        entityManager.persistAndFlush(techBook1);
        entityManager.persistAndFlush(techBook2);
        entityManager.persistAndFlush(novelBook1);
        entityManager.persistAndFlush(businessBook1);
    }

    @Test
    void findByCategoryContainingIgnoreCase_WithExistingCategory_ShouldReturnBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByCategoryContainingIgnoreCase("技術", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Book::getCategory)
                .allMatch(category -> category.contains("技術"));
        assertThat(result.getContent())
                .extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Spring Boot in Action", "Java: The Complete Reference");
    }

    @Test
    void findByCategoryContainingIgnoreCase_WithCaseInsensitiveSearch_ShouldReturnBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByCategoryContainingIgnoreCase("技術書", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Book::getCategory)
                .allMatch("技術書"::equals);
    }

    @Test
    void findByCategoryContainingIgnoreCase_WithNonExistentCategory_ShouldReturnEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByCategoryContainingIgnoreCase("存在しないカテゴリー", pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findByTitleOrAuthorContaining_WithTitleKeyword_ShouldReturnMatchingBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByTitleOrAuthorContaining("Spring", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Spring");
    }

    @Test
    void findByTitleOrAuthorContaining_WithAuthorKeyword_ShouldReturnMatchingBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByTitleOrAuthorContaining("Murakami", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor()).contains("Murakami");
    }

    @Test
    void findByTitleOrAuthorContaining_WithCaseInsensitiveKeyword_ShouldReturnMatchingBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByTitleOrAuthorContaining("java", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).containsIgnoringCase("Java");
    }

    @Test
    void findByTitleOrAuthorContaining_WithNonExistentKeyword_ShouldReturnEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByTitleOrAuthorContaining("NonExistentKeyword", pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByCategoryAndKeyword_WithMatchingCategoryAndKeyword_ShouldReturnBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByCategoryAndKeyword("技術", "Spring", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Spring");
        assertThat(result.getContent().get(0).getCategory()).contains("技術");
    }

    @Test
    void findByCategoryAndKeyword_WithNonMatchingCategory_ShouldReturnEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByCategoryAndKeyword("小説", "Spring", pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByStockQuantityGreaterThan_WithMinimumStock_ShouldReturnInStockBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByStockQuantityGreaterThan(0, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3); // All except novelBook1 (stock = 0)
        assertThat(result.getContent())
                .extracting(Book::getStockQuantity)
                .allMatch(stock -> stock > 0);
    }

    @Test
    void findByStockQuantityGreaterThan_WithHighMinimumStock_ShouldReturnLimitedBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByStockQuantityGreaterThan(7, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2); // techBook1 (10) and businessBook1 (8)
        assertThat(result.getContent())
                .extracting(Book::getStockQuantity)
                .allMatch(stock -> stock > 7);
    }

    @Test
    void findByPriceBetween_WithPriceRange_ShouldReturnBooksInRange() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByPriceBetween(2000, 4000, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2); // businessBook1 (2500) and techBook1 (3500)
        assertThat(result.getContent())
                .extracting(book -> book.getPrice().intValue())
                .allMatch(price -> price >= 2000 && price <= 4000);
    }

    @Test
    void findByPriceBetween_WithNarrowRange_ShouldReturnLimitedBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findByPriceBetween(1000, 2000, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1); // Only novelBook1 (1800)
        assertThat(result.getContent().get(0).getPrice().intValue()).isBetween(1000, 2000);
    }

    @Test
    void findTopRatedBooks_ShouldReturnBooksOrderedByRating() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Book> result = bookRepository.findTopRatedBooks(pageable);

        // Then
        assertThat(result).hasSize(4);
        // Should be ordered by rating descending: novelBook1 (4.8), techBook1 (4.5), techBook2 (4.2), businessBook1 (4.0)
        assertThat(result.get(0).getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.8));
        assertThat(result.get(1).getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(result.get(2).getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.2));
        assertThat(result.get(3).getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
    }

    @Test
    void findTopRatedBooks_WithLimit_ShouldReturnLimitedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<Book> result = bookRepository.findTopRatedBooks(pageable);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.8));
        assertThat(result.get(1).getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void findPopularBooks_ShouldReturnBooksOrderedByReviewCount() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Book> result = bookRepository.findPopularBooks(pageable);

        // Then
        assertThat(result).hasSize(4);
        // Should be ordered by review count descending: novelBook1 (50), businessBook1 (30), techBook1 (25), techBook2 (15)
        assertThat(result.get(0).getReviewCount()).isEqualTo(50);
        assertThat(result.get(1).getReviewCount()).isEqualTo(30);
        assertThat(result.get(2).getReviewCount()).isEqualTo(25);
        assertThat(result.get(3).getReviewCount()).isEqualTo(15);
    }

    @Test
    void findPopularBooks_WithLimit_ShouldReturnLimitedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<Book> result = bookRepository.findPopularBooks(pageable);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReviewCount()).isEqualTo(50);
        assertThat(result.get(1).getReviewCount()).isEqualTo(30);
    }

    @Test
    void findAllCategories_ShouldReturnDistinctCategoriesOrderedAlphabetically() {
        // When
        List<String> result = bookRepository.findAllCategories();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("ビジネス", "小説", "技術書"); // Ordered alphabetically in Japanese
    }

    @Test
    void findAllCategories_WithDuplicateCategories_ShouldReturnDistinctOnly() {
        // Given - add another book with existing category
        Book anotherTechBook = Book.builder()
                .title("Another Tech Book")
                .author("Another Author")
                .isbn("9999999999999")
                .price(BigDecimal.valueOf(3000))
                .category("技術書") // Same category as existing books
                .stockQuantity(1)
                .averageRating(BigDecimal.valueOf(4.0))
                .reviewCount(1)
                .build();
        entityManager.persistAndFlush(anotherTechBook);

        // When
        List<String> result = bookRepository.findAllCategories();

        // Then
        assertThat(result).hasSize(3); // Still 3 distinct categories
        assertThat(result).containsExactly("ビジネス", "小説", "技術書");
    }

    @Test
    void findByIsbn_WithExistingIsbn_ShouldReturnBook() {
        // When
        Optional<Book> result = bookRepository.findByIsbn("9781617292545");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Spring Boot in Action");
    }

    @Test
    void findByIsbn_WithNonExistentIsbn_ShouldReturnEmpty() {
        // When
        Optional<Book> result = bookRepository.findByIsbn("9999999999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findBooksWithMinimumStock_ShouldReturnBooksOrderedByStock() {
        // Given
        Integer minStock = 3;

        // When
        List<Book> result = bookRepository.findBooksWithMinimumStock(minStock);

        // Then
        assertThat(result).hasSize(3); // techBook2 (5), businessBook1 (8), techBook1 (10)
        assertThat(result)
                .extracting(Book::getStockQuantity)
                .allMatch(stock -> stock > minStock);
        
        // Should be ordered by stock quantity ascending
        assertThat(result.get(0).getStockQuantity()).isEqualTo(5);
        assertThat(result.get(1).getStockQuantity()).isEqualTo(8);
        assertThat(result.get(2).getStockQuantity()).isEqualTo(10);
    }

    @Test
    void findBooksWithMinimumStock_WithHighMinStock_ShouldReturnLimitedBooks() {
        // Given
        Integer minStock = 8;

        // When
        List<Book> result = bookRepository.findBooksWithMinimumStock(minStock);

        // Then
        assertThat(result).hasSize(1); // Only techBook1 (10)
        assertThat(result.get(0).getStockQuantity()).isEqualTo(10);
    }

    @Test
    void findBooksWithMinimumStock_WithVeryHighMinStock_ShouldReturnEmpty() {
        // Given
        Integer minStock = 20;

        // When
        List<Book> result = bookRepository.findBooksWithMinimumStock(minStock);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void repository_BasicCrudOperations_ShouldWork() {
        // Test save
        Book newBook = Book.builder()
                .title("Test Book")
                .author("Test Author")
                .isbn("1111111111111")
                .price(BigDecimal.valueOf(1000))
                .category("テスト")
                .stockQuantity(1)
                .averageRating(BigDecimal.valueOf(5.0))
                .reviewCount(1)
                .build();

        Book saved = bookRepository.save(newBook);
        assertThat(saved.getId()).isNotNull();

        // Test findById
        Optional<Book> found = bookRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Book");

        // Test update
        found.get().setTitle("Updated Test Book");
        Book updated = bookRepository.save(found.get());
        assertThat(updated.getTitle()).isEqualTo("Updated Test Book");

        // Test delete
        bookRepository.delete(updated);
        Optional<Book> deleted = bookRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    void findAll_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Book> result = bookRepository.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }
}