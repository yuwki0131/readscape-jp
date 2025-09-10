package jp.readscape.consumer.domain.orders.repository;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.orders.model.Order;
import jp.readscape.consumer.domain.orders.model.OrderItem;
import jp.readscape.consumer.domain.orders.model.OrderStatus;
import jp.readscape.consumer.domain.orders.model.PaymentMethod;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OrderRepository Test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private User testUser;
    private Book testBook1;
    private Book testBook2;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // テストユーザー作成
        testUser = User.builder()
            .email("order-test@example.com")
            .password("hashedPassword")
            .name("注文テストユーザー")
            .role(UserRole.CONSUMER)
            .active(true)
            .emailVerified(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        testUser = entityManager.persistAndFlush(testUser);

        // テスト書籍作成
        testBook1 = Book.builder()
            .title("リポジトリテスト書籍1")
            .author("テスト著者1")
            .isbn("9784000001001")
            .price(new BigDecimal("2000"))
            .category("テスト")
            .description("リポジトリテスト用書籍1")
            .publisher("テスト出版")
            .pages(300)
            .publicationDate(LocalDate.now())
            .stockQuantity(50)
            .inStock(true)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        testBook1 = entityManager.persistAndFlush(testBook1);

        testBook2 = Book.builder()
            .title("リポジトリテスト書籍2")
            .author("テスト著者2")
            .isbn("9784000001002")
            .price(new BigDecimal("3500"))
            .category("テスト")
            .stockQuantity(30)
            .inStock(true)
            .active(true)
            .build();
        testBook2 = entityManager.persistAndFlush(testBook2);

        // テスト注文作成
        OrderItem item1 = OrderItem.builder()
            .bookId(testBook1.getId())
            .book(testBook1)
            .quantity(2)
            .unitPrice(testBook1.getPrice())
            .totalPrice(testBook1.getPrice().multiply(new BigDecimal("2")))
            .build();

        OrderItem item2 = OrderItem.builder()
            .bookId(testBook2.getId())
            .book(testBook2)
            .quantity(1)
            .unitPrice(testBook2.getPrice())
            .totalPrice(testBook2.getPrice())
            .build();

        testOrder = Order.builder()
            .orderNumber("ORD-TEST-001")
            .userId(testUser.getId())
            .status(OrderStatus.PENDING)
            .items(Arrays.asList(item1, item2))
            .subtotal(new BigDecimal("7500")) // 2000*2 + 3500*1
            .shippingFee(new BigDecimal("500"))
            .tax(new BigDecimal("750"))
            .totalAmount(new BigDecimal("8750"))
            .paymentMethod(PaymentMethod.CREDIT_CARD)
            .shippingAddress("テスト配送先住所")
            .notes("テスト注文メモ")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // OrderItemにOrderを設定
        item1.setOrder(testOrder);
        item2.setOrder(testOrder);

        testOrder = entityManager.persistAndFlush(testOrder);
    }

    @Test
    @DisplayName("注文をIDで検索 - 正常系")
    void findByIdSuccess() {
        // Act
        Optional<Order> found = orderRepository.findById(testOrder.getId());

        // Assert
        assertThat(found).isPresent();
        Order order = found.get();
        assertThat(order.getOrderNumber()).isEqualTo("ORD-TEST-001");
        assertThat(order.getUserId()).isEqualTo(testUser.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("8750"));
    }

    @Test
    @DisplayName("注文番号で検索 - 正常系")
    void findByOrderNumberSuccess() {
        // Act
        Optional<Order> found = orderRepository.findByOrderNumber("ORD-TEST-001");

        // Assert
        assertThat(found).isPresent();
        Order order = found.get();
        assertThat(order.getId()).isEqualTo(testOrder.getId());
        assertThat(order.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("ユーザーIDで注文一覧検索 - ページング")
    void findByUserIdWithPaging() {
        // Arrange: 追加の注文データを作成
        createAdditionalOrders();

        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        // Act
        Page<Order> orders = orderRepository.findByUserId(testUser.getId(), pageable);

        // Assert
        assertThat(orders.getContent()).hasSize(2);
        assertThat(orders.getTotalElements()).isEqualTo(3);
        assertThat(orders.getTotalPages()).isEqualTo(2);
        assertThat(orders.hasNext()).isTrue();
        
        // 最新の注文が最初に来ることを確認
        Order firstOrder = orders.getContent().get(0);
        assertThat(firstOrder.getCreatedAt()).isAfter(testOrder.getCreatedAt());
    }

    @Test
    @DisplayName("ステータス別注文検索")
    void findByUserIdAndStatus() {
        // Arrange: 異なるステータスの注文を作成
        Order confirmedOrder = createOrderWithStatus(OrderStatus.CONFIRMED);
        Order shippedOrder = createOrderWithStatus(OrderStatus.SHIPPED);

        // Act
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(
            testUser.getId(), OrderStatus.PENDING);
        List<Order> confirmedOrders = orderRepository.findByUserIdAndStatus(
            testUser.getId(), OrderStatus.CONFIRMED);
        List<Order> shippedOrders = orderRepository.findByUserIdAndStatus(
            testUser.getId(), OrderStatus.SHIPPED);

        // Assert
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getId()).isEqualTo(testOrder.getId());

        assertThat(confirmedOrders).hasSize(1);
        assertThat(confirmedOrders.get(0).getId()).isEqualTo(confirmedOrder.getId());

        assertThat(shippedOrders).hasSize(1);
        assertThat(shippedOrders.get(0).getId()).isEqualTo(shippedOrder.getId());
    }

    @Test
    @DisplayName("期間指定での注文検索")
    void findByDateRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // Act
        List<Order> ordersInRange = orderRepository.findByCreatedAtBetween(startDate, endDate);

        // Assert
        assertThat(ordersInRange).hasSize(1);
        assertThat(ordersInRange.get(0).getId()).isEqualTo(testOrder.getId());
    }

    @Test
    @DisplayName("期間外の注文は検索されない")
    void findByDateRangeOutsideRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().minusDays(5);

        // Act
        List<Order> ordersInRange = orderRepository.findByCreatedAtBetween(startDate, endDate);

        // Assert
        assertThat(ordersInRange).isEmpty();
    }

    @Test
    @DisplayName("支払い方法別注文検索")
    void findByPaymentMethod() {
        // Arrange: 異なる支払い方法の注文を作成
        Order bankTransferOrder = Order.builder()
            .orderNumber("ORD-BANK-001")
            .userId(testUser.getId())
            .status(OrderStatus.PENDING)
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .totalAmount(new BigDecimal("5000"))
            .createdAt(LocalDateTime.now())
            .build();
        entityManager.persistAndFlush(bankTransferOrder);

        // Act
        List<Order> creditCardOrders = orderRepository.findByPaymentMethod(PaymentMethod.CREDIT_CARD);
        List<Order> bankTransferOrders = orderRepository.findByPaymentMethod(PaymentMethod.BANK_TRANSFER);

        // Assert
        assertThat(creditCardOrders).hasSize(1);
        assertThat(creditCardOrders.get(0).getId()).isEqualTo(testOrder.getId());

        assertThat(bankTransferOrders).hasSize(1);
        assertThat(bankTransferOrders.get(0).getId()).isEqualTo(bankTransferOrder.getId());
    }

    @Test
    @DisplayName("金額範囲での注文検索")
    void findByTotalAmountRange() {
        // Arrange: 異なる金額の注文を作成
        Order lowAmountOrder = Order.builder()
            .orderNumber("ORD-LOW-001")
            .userId(testUser.getId())
            .status(OrderStatus.PENDING)
            .totalAmount(new BigDecimal("1000"))
            .createdAt(LocalDateTime.now())
            .build();
        entityManager.persistAndFlush(lowAmountOrder);

        Order highAmountOrder = Order.builder()
            .orderNumber("ORD-HIGH-001")
            .userId(testUser.getId())
            .status(OrderStatus.PENDING)
            .totalAmount(new BigDecimal("50000"))
            .createdAt(LocalDateTime.now())
            .build();
        entityManager.persistAndFlush(highAmountOrder);

        // Act
        List<Order> midRangeOrders = orderRepository.findByTotalAmountBetween(
            new BigDecimal("5000"), new BigDecimal("10000"));

        // Assert
        assertThat(midRangeOrders).hasSize(1);
        assertThat(midRangeOrders.get(0).getId()).isEqualTo(testOrder.getId());
    }

    @Test
    @DisplayName("書籍IDを含む注文検索")
    void findByBookId() {
        // Act
        List<Order> ordersWithBook1 = orderRepository.findOrdersContainingBook(testBook1.getId());
        List<Order> ordersWithBook2 = orderRepository.findOrdersContainingBook(testBook2.getId());

        // Assert
        assertThat(ordersWithBook1).hasSize(1);
        assertThat(ordersWithBook1.get(0).getId()).isEqualTo(testOrder.getId());

        assertThat(ordersWithBook2).hasSize(1);
        assertThat(ordersWithBook2.get(0).getId()).isEqualTo(testOrder.getId());
    }

    @Test
    @DisplayName("注文統計情報取得")
    void getOrderStatistics() {
        // Arrange: 追加データ作成
        createOrderWithStatus(OrderStatus.CONFIRMED);
        createOrderWithStatus(OrderStatus.SHIPPED);
        createOrderWithStatus(OrderStatus.CANCELLED);

        // Act
        Long totalOrders = orderRepository.count();
        Long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        Long confirmedCount = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        Long shippedCount = orderRepository.countByStatus(OrderStatus.SHIPPED);
        Long cancelledCount = orderRepository.countByStatus(OrderStatus.CANCELLED);

        // Assert
        assertThat(totalOrders).isEqualTo(4);
        assertThat(pendingCount).isEqualTo(1);
        assertThat(confirmedCount).isEqualTo(1);
        assertThat(shippedCount).isEqualTo(1);
        assertThat(cancelledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("ユーザーの総注文金額計算")
    void calculateUserTotalAmount() {
        // Arrange: 同一ユーザーで追加注文作成
        Order additionalOrder = Order.builder()
            .orderNumber("ORD-ADDITIONAL-001")
            .userId(testUser.getId())
            .status(OrderStatus.DELIVERED)
            .totalAmount(new BigDecimal("15000"))
            .createdAt(LocalDateTime.now())
            .build();
        entityManager.persistAndFlush(additionalOrder);

        // Act
        BigDecimal totalAmount = orderRepository.calculateUserTotalAmount(testUser.getId());

        // Assert
        assertThat(totalAmount).isEqualByComparingTo(new BigDecimal("23750")); // 8750 + 15000
    }

    @Test
    @DisplayName("最近の注文一覧取得")
    void findRecentOrders() {
        // Arrange
        createAdditionalOrders();

        // Act
        List<Order> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc();

        // Assert
        assertThat(recentOrders).hasSize(3);
        
        // 作成日時の降順で並んでいることを確認
        for (int i = 0; i < recentOrders.size() - 1; i++) {
            assertThat(recentOrders.get(i).getCreatedAt())
                .isAfterOrEqualTo(recentOrders.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("注文データの制約条件テスト")
    void orderConstraintsTest() {
        // 必須フィールドの検証
        Order invalidOrder = Order.builder().build();
        
        // 必須フィールドが不足した注文は保存できない
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(invalidOrder);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("カスケード削除テスト")
    void cascadeDeleteTest() {
        // 注文削除時にOrderItemも削除されることを確認
        Long orderId = testOrder.getId();
        Long orderItemCount = (Long) entityManager.getEntityManager()
            .createQuery("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
            .setParameter("orderId", orderId)
            .getSingleResult();
        
        assertThat(orderItemCount).isEqualTo(2);

        // 注文削除
        orderRepository.delete(testOrder);
        entityManager.flush();

        // OrderItemも削除されていることを確認
        Long remainingOrderItemCount = (Long) entityManager.getEntityManager()
            .createQuery("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
            .setParameter("orderId", orderId)
            .getSingleResult();
        
        assertThat(remainingOrderItemCount).isEqualTo(0);
    }

    // Helper methods
    private void createAdditionalOrders() {
        Order order2 = Order.builder()
            .orderNumber("ORD-TEST-002")
            .userId(testUser.getId())
            .status(OrderStatus.CONFIRMED)
            .totalAmount(new BigDecimal("5000"))
            .createdAt(LocalDateTime.now().plusMinutes(10))
            .build();

        Order order3 = Order.builder()
            .orderNumber("ORD-TEST-003")
            .userId(testUser.getId())
            .status(OrderStatus.SHIPPED)
            .totalAmount(new BigDecimal("12000"))
            .createdAt(LocalDateTime.now().plusMinutes(20))
            .build();

        entityManager.persistAndFlush(order2);
        entityManager.persistAndFlush(order3);
    }

    private Order createOrderWithStatus(OrderStatus status) {
        Order order = Order.builder()
            .orderNumber("ORD-" + status + "-001")
            .userId(testUser.getId())
            .status(status)
            .totalAmount(new BigDecimal("7500"))
            .createdAt(LocalDateTime.now())
            .build();

        return entityManager.persistAndFlush(order);
    }
}