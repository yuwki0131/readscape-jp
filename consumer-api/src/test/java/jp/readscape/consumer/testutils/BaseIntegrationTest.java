package jp.readscape.consumer.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.cart.repository.CartRepository;
import jp.readscape.consumer.domain.orders.repository.OrderRepository;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 統合テスト用のベースクラス
 * 共通的な設定とセットアップを提供
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BookRepository bookRepository;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected JwtService jwtService;

    // テストデータ管理
    protected TestDataManager dataManager;

    @BeforeEach
    void baseSetUp() {
        dataManager = new TestDataManager(
            userRepository, bookRepository, cartRepository, orderRepository
        );
        cleanupTestData();
    }

    /**
     * テストデータのクリーンアップ
     */
    protected void cleanupTestData() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * JWT認証トークンを生成
     */
    protected String generateAuthToken(String email) {
        return "Bearer " + jwtService.generateToken(email);
    }

    /**
     * 管理者用JWT認証トークンを生成
     */
    protected String generateAdminToken(String email) {
        return "Bearer " + jwtService.generateToken(email);
    }

    /**
     * テストデータ管理クラス
     */
    protected static class TestDataManager {
        private final UserRepository userRepository;
        private final BookRepository bookRepository;
        private final CartRepository cartRepository;
        private final OrderRepository orderRepository;

        public TestDataManager(UserRepository userRepository, BookRepository bookRepository,
                             CartRepository cartRepository, OrderRepository orderRepository) {
            this.userRepository = userRepository;
            this.bookRepository = bookRepository;
            this.cartRepository = cartRepository;
            this.orderRepository = orderRepository;
        }

        /**
         * 基本的なテストデータセットを準備
         */
        public BasicTestDataSet prepareBasicDataSet() {
            var user = userRepository.save(TestDataFactory.createUser());
            var books = bookRepository.saveAll(TestDataFactory.createBooks(5));
            return new BasicTestDataSet(user, books);
        }

        /**
         * ショッピング用のテストデータセットを準備
         */
        public ShoppingTestDataSet prepareShoppingDataSet() {
            var scenario = new TestScenarios.ShoppingCartScenario();
            var user = userRepository.save(scenario.customer);
            var books = bookRepository.saveAll(scenario.availableBooks);
            var cart = cartRepository.save(TestDataFactory.createCart(user.getId(), books));
            return new ShoppingTestDataSet(user, books, cart);
        }

        /**
         * 注文処理用のテストデータセットを準備
         */
        public OrderTestDataSet prepareOrderDataSet() {
            var scenario = new TestScenarios.OrderProcessingScenario();
            var user = userRepository.save(scenario.customer);
            var books = bookRepository.saveAll(scenario.orderBooks);
            var orders = orderRepository.saveAll(
                java.util.List.of(scenario.pendingOrder, scenario.confirmedOrder, scenario.shippedOrder)
            );
            return new OrderTestDataSet(user, books, orders);
        }

        /**
         * パフォーマンステスト用の大量データを準備
         */
        public PerformanceTestDataSet preparePerformanceDataSet(int userCount, int bookCount) {
            System.out.println("Preparing performance test data...");
            var users = userRepository.saveAll(TestDataFactory.createUsers(userCount));
            var books = bookRepository.saveAll(TestDataFactory.createBooks(bookCount));
            System.out.println("Performance test data preparation completed.");
            return new PerformanceTestDataSet(users, books);
        }
    }

    // データセット用のレコードクラス
    protected record BasicTestDataSet(
        jp.readscape.consumer.domain.users.model.User user,
        java.util.List<jp.readscape.consumer.domain.books.model.Book> books
    ) {}

    protected record ShoppingTestDataSet(
        jp.readscape.consumer.domain.users.model.User user,
        java.util.List<jp.readscape.consumer.domain.books.model.Book> books,
        jp.readscape.consumer.domain.cart.model.Cart cart
    ) {}

    protected record OrderTestDataSet(
        jp.readscape.consumer.domain.users.model.User user,
        java.util.List<jp.readscape.consumer.domain.books.model.Book> books,
        java.util.List<jp.readscape.consumer.domain.orders.model.Order> orders
    ) {}

    protected record PerformanceTestDataSet(
        java.util.List<jp.readscape.consumer.domain.users.model.User> users,
        java.util.List<jp.readscape.consumer.domain.books.model.Book> books
    ) {}
}