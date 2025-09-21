package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.cart.model.Cart;
import jp.readscape.consumer.domain.cart.model.CartItem;
import jp.readscape.consumer.domain.cart.repository.CartRepository;
import jp.readscape.consumer.domain.orders.model.Order;
import jp.readscape.consumer.domain.orders.model.OrderItem;
import jp.readscape.consumer.domain.orders.repository.OrderRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.orders.CreateOrderRequest;
import jp.readscape.consumer.dto.orders.CreateOrderResponse;
import jp.readscape.consumer.dto.orders.OrderDetail;
import jp.readscape.consumer.dto.users.OrderSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Book testBook;
    private Cart testCart;
    private CartItem testCartItem;
    private Order testOrder;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser@example.com")
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("9781234567890")
                .price(1500)
                .stockQuantity(10)
                .build();

        testCartItem = CartItem.builder()
                .id(1L)
                .book(testBook)
                .quantity(2)
                .unitPrice(1500)
                .build();

        testCart = Cart.builder()
                .id(1L)
                .user(testUser)
                .items(Arrays.asList(testCartItem))
                .build();
        testCartItem.setCart(testCart);

        testOrder = Order.builder()
                .id(1L)
                .user(testUser)
                .orderNumber("ORD-20231201-0001")
                .status(Order.OrderStatus.PENDING)
                .shippingAddress("Test Address")
                .shippingPhone("090-1234-5678")
                .paymentMethod("CREDIT_CARD")
                .totalAmount(BigDecimal.valueOf(3000))
                .orderDate(LocalDateTime.now())
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .shippingAddress("Test Address")
                .shippingPhone("090-1234-5678")
                .paymentMethod("CREDIT_CARD")
                .notes("Test notes")
                .build();
    }

    @Test
    void createOrderFromCart_WithValidCart_ShouldCreateOrderSuccessfully() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(bookRepository.saveAll(anyList())).thenReturn(Arrays.asList(testBook));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        CreateOrderResponse response = orderService.createOrderFromCart(1L, createOrderRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getOrderNumber()).isEqualTo("ORD-20231201-0001");
        
        verify(cartRepository).findByUserId(1L);
        verify(orderRepository).save(any(Order.class));
        verify(bookRepository).saveAll(anyList());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void createOrderFromCart_WithNonExistentCart_ShouldThrowException() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, createOrderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("カートが見つかりません");
    }

    @Test
    void createOrderFromCart_WithEmptyCart_ShouldThrowException() {
        // Given
        Cart emptyCart = Cart.builder()
                .id(1L)
                .user(testUser)
                .items(Collections.emptyList())
                .build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, createOrderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("カートが空です");
    }

    @Test
    void createOrderFromCart_WithInsufficientStock_ShouldThrowException() {
        // Given
        testBook.setStockQuantity(1); // Less than required quantity (2)
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, createOrderRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("在庫が不足しています");
    }

    @Test
    void createOrderFromCart_WithNullStock_ShouldThrowException() {
        // Given
        testBook.setStockQuantity(null);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, createOrderRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("在庫が不足しています");
    }

    @Test
    void createOrderFromCart_WithOrderSaveFailure_ShouldThrowException() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, createOrderRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("注文の作成に失敗しました");
    }

    @Test
    void getOrderDetail_WithValidOrderAndUser_ShouldReturnOrderDetail() {
        // Given
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));

        // When
        OrderDetail result = orderService.getOrderDetail(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).findByIdAndUserId(1L, 1L);
    }

    @Test
    void getOrderDetail_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderDetail(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("注文が見つかりません");
    }

    @Test
    void getUserOrders_WithValidUser_ShouldReturnOrderList() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L)).thenReturn(orders);

        // When
        List<OrderSummary> result = orderService.getUserOrders(1L);

        // Then
        assertThat(result).hasSize(1);
        verify(orderRepository).findByUserIdOrderByOrderDateDesc(1L);
    }

    @Test
    void getRecentUserOrders_WithValidUserAndLimit_ShouldReturnLimitedOrders() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findRecentOrdersByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(orders);

        // When
        List<OrderSummary> result = orderService.getRecentUserOrders(1L, 5);

        // Then
        assertThat(result).hasSize(1);
        verify(orderRepository).findRecentOrdersByUserId(eq(1L), eq(PageRequest.of(0, 5)));
    }

    @Test
    void updateOrderStatus_WithValidOrder_ShouldUpdateStatus() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED);

        // Then
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("注文が見つかりません");
    }

    @Test
    void cancelOrder_WithCancellableOrder_ShouldCancelSuccessfully() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING); // Cancellable status
        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(testOrder)
                .book(testBook)
                .quantity(2)
                .build();
        testOrder.setItems(Arrays.asList(orderItem));

        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        orderService.cancelOrder(1L, 1L);

        // Then
        verify(orderRepository).findByIdAndUserId(1L, 1L);
        verify(orderRepository).save(testOrder);
        verify(bookRepository).save(testBook);
        assertThat(testOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_WithNonCancellableOrder_ShouldThrowException() {
        // Given
        testOrder.setStatus(Order.OrderStatus.DELIVERED); // Non-cancellable status
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("この注文はキャンセルできません");
    }

    @Test
    void cancelOrder_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("注文が見つかりません");
    }

    @Test
    void getOrderStatistics_WithValidUser_ShouldReturnStatistics() {
        // Given
        when(orderRepository.countOrdersByUserId(1L)).thenReturn(5L);
        when(orderRepository.getTotalAmountByUserId(1L)).thenReturn(15000.0);

        // When
        OrderService.OrderStatistics statistics = orderService.getOrderStatistics(1L);

        // Then
        assertThat(statistics.getOrderCount()).isEqualTo(5L);
        assertThat(statistics.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(15000.0));
        assertThat(statistics.getFormattedTotalAmount()).isEqualTo("¥15,000");
    }

    @Test
    void getOrderStatistics_WithNoOrders_ShouldReturnZeroStatistics() {
        // Given
        when(orderRepository.countOrdersByUserId(1L)).thenReturn(0L);
        when(orderRepository.getTotalAmountByUserId(1L)).thenReturn(null);

        // When
        OrderService.OrderStatistics statistics = orderService.getOrderStatistics(1L);

        // Then
        assertThat(statistics.getOrderCount()).isEqualTo(0L);
        assertThat(statistics.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(statistics.getFormattedTotalAmount()).isEqualTo("¥0");
    }

    @Test
    void createOrderFromCart_ShouldUpdateBookStockCorrectly() {
        // Given
        Integer originalStock = 10;
        Integer orderQuantity = 2;
        testBook.setStockQuantity(originalStock);
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(bookRepository.saveAll(anyList())).thenReturn(Arrays.asList(testBook));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        orderService.createOrderFromCart(1L, createOrderRequest);

        // Then
        verify(bookRepository).saveAll(anyList());
        assertThat(testBook.getStockQuantity()).isEqualTo(originalStock - orderQuantity);
    }

    @Test
    void cancelOrder_ShouldRestoreBookStockCorrectly() {
        // Given
        Integer currentStock = 8;
        Integer orderQuantity = 2;
        testBook.setStockQuantity(currentStock);
        testOrder.setStatus(Order.OrderStatus.PENDING);
        
        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(testOrder)
                .book(testBook)
                .quantity(orderQuantity)
                .build();
        testOrder.setItems(Arrays.asList(orderItem));

        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        orderService.cancelOrder(1L, 1L);

        // Then
        verify(bookRepository).save(testBook);
        assertThat(testBook.getStockQuantity()).isEqualTo(currentStock + orderQuantity);
    }

    @Test
    void cancelOrder_WithNullBook_ShouldHandleGracefully() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING);
        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(testOrder)
                .book(null) // Null book
                .quantity(2)
                .build();
        testOrder.setItems(Arrays.asList(orderItem));

        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When & Then - should not throw exception
        assertThatCode(() -> orderService.cancelOrder(1L, 1L))
                .doesNotThrowAnyException();
        
        verify(orderRepository).save(testOrder);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void createOrderFromCart_WithMultipleBooks_ShouldHandleAllBooks() {
        // Given
        Book secondBook = Book.builder()
                .id(2L)
                .title("Second Book")
                .author("Second Author")
                .isbn("9780987654321")
                .price(2000)
                .stockQuantity(5)
                .build();

        CartItem secondCartItem = CartItem.builder()
                .id(2L)
                .book(secondBook)
                .quantity(1)
                .unitPrice(2000)
                .build();

        testCart.getItems().add(secondCartItem);
        secondCartItem.setCart(testCart);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(bookRepository.saveAll(anyList())).thenReturn(Arrays.asList(testBook, secondBook));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        orderService.createOrderFromCart(1L, createOrderRequest);

        // Then
        verify(bookRepository).saveAll(argThat(books -> {
            List<Book> bookList = (List<Book>) books;
            return bookList.size() == 2 &&
                   bookList.get(0).getStockQuantity() == 8 && // 10 - 2
                   bookList.get(1).getStockQuantity() == 4;   // 5 - 1
        }));
    }

    @Test
    void createOrderFromCart_WithZeroStockAfterOrder_ShouldSetToZero() {
        // Given
        testBook.setStockQuantity(2);
        testCartItem.setQuantity(3); // More than available
        
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(bookRepository.saveAll(anyList())).thenReturn(Arrays.asList(testBook));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        orderService.createOrderFromCart(1L, createOrderRequest);

        // Then
        // Should not reduce stock below zero
        assertThat(testBook.getStockQuantity()).isZero();
    }
}