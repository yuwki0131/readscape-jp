package jp.readscape.consumer.configurations;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing test data...");

        // Initialize test users if they don't exist
        if (userRepository.count() == 0) {
            initializeUsers();
        }

        // Initialize test books if they don't exist
        if (bookRepository.count() == 0) {
            initializeBooks();
        }

        log.info("Data initialization completed.");
    }

    private void initializeUsers() {
        String hashedPassword = passwordEncoder.encode("testpass");
        LocalDateTime now = LocalDateTime.now();

        User consumer1 = User.builder()
                .username("consumer1")
                .email("consumer1@readscape.jp")
                .password(hashedPassword)
                .firstName("一般")
                .lastName("消費者1")
                .phone("090-1234-5678")
                .role(UserRole.CONSUMER)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User manager1 = User.builder()
                .username("manager1")
                .email("manager1@readscape.jp")
                .password(hashedPassword)
                .firstName("管理")
                .lastName("者1")
                .phone("090-1234-5679")
                .role(UserRole.MANAGER)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User admin1 = User.builder()
                .username("admin1")
                .email("admin1@readscape.jp")
                .password(hashedPassword)
                .firstName("システム")
                .lastName("管理者")
                .phone("090-1234-5680")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(consumer1);
        userRepository.save(manager1);
        userRepository.save(admin1);

        log.info("Test users initialized: consumer1, manager1, admin1");
    }

    private void initializeBooks() {
        LocalDateTime now = LocalDateTime.now();

        Book book1 = Book.builder()
                .title("Spring Boot実践入門")
                .author("技術太郎")
                .isbn("9784000000001")
                .price(3200)
                .description("Spring Bootの基本から応用まで幅広く解説した実践的な入門書です。")
                .category("技術書")
                .stockQuantity(25)
                .averageRating(BigDecimal.valueOf(4.5))
                .reviewCount(12)
                .imageUrl("https://images.readscape.jp/books/spring-boot.jpg")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Book book2 = Book.builder()
                .title("Java設計パターン")
                .author("パターン花子")
                .isbn("9784000000002")
                .price(2800)
                .description("GoFパターンをJavaで実践的に学ぶための決定版。")
                .category("技術書")
                .stockQuantity(15)
                .averageRating(BigDecimal.valueOf(4.2))
                .reviewCount(8)
                .imageUrl("https://images.readscape.jp/books/java-patterns.jpg")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Book book3 = Book.builder()
                .title("マーケティング戦略入門")
                .author("ビジネス次郎")
                .isbn("9784000000003")
                .price(2200)
                .description("現代マーケティングの基礎から実践まで学べる入門書。")
                .category("ビジネス書")
                .stockQuantity(30)
                .averageRating(BigDecimal.valueOf(4.0))
                .reviewCount(5)
                .imageUrl("https://images.readscape.jp/books/marketing.jpg")
                .createdAt(now)
                .updatedAt(now)
                .build();

        bookRepository.save(book1);
        bookRepository.save(book2);
        bookRepository.save(book3);

        log.info("Test books initialized: {} books", 3);
    }
}