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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;

    /**
     * カートから注文を作成
     */
    @Transactional
    public CreateOrderResponse createOrderFromCart(Long userId, CreateOrderRequest request) {
        log.debug("Creating order from cart for user: {}", userId);

        // ユーザーのカートを取得
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("カートが見つかりません"));

        if (cart.isEmpty()) {
            throw new IllegalArgumentException("カートが空です");
        }

        // 在庫チェックと予約
        validateAndReserveStock(cart);

        try {
            // 注文を作成
            Order order = createOrderFromCart(cart, request);
            Order savedOrder = orderRepository.save(order);

            // 在庫を減算
            updateBookStock(cart);

            // カートをクリア
            cart.clear();
            cartRepository.save(cart);

            log.info("Order created successfully: {} for user: {}", savedOrder.getOrderNumber(), userId);
            return CreateOrderResponse.from(savedOrder);

        } catch (Exception e) {
            log.error("Failed to create order for user {}: {}", userId, e.getMessage());
            // 在庫予約をロールバック（トランザクションが自動的に処理）
            throw new RuntimeException("注文の作成に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * 注文詳細を取得
     */
    public OrderDetail getOrderDetail(Long orderId, Long userId) {
        log.debug("Getting order detail: {} for user: {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("注文が見つかりません"));

        return OrderDetail.from(order);
    }

    /**
     * ユーザーの注文履歴を取得
     */
    public List<OrderSummary> getUserOrders(Long userId) {
        log.debug("Getting orders for user: {}", userId);

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        
        return orders.stream()
                .map(OrderSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * 最近の注文を取得
     */
    public List<OrderSummary> getRecentUserOrders(Long userId, int limit) {
        log.debug("Getting recent orders for user: {} (limit: {})", userId, limit);

        List<Order> orders = orderRepository.findRecentOrdersByUserId(
                userId, 
                PageRequest.of(0, limit)
        );
        
        return orders.stream()
                .map(OrderSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * 注文ステータスを更新
     */
    @Transactional
    public void updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        log.debug("Updating order status: {} to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("注文が見つかりません"));

        order.updateStatus(newStatus);
        orderRepository.save(order);

        log.info("Order status updated: {} -> {}", orderId, newStatus);
    }

    /**
     * 注文をキャンセル
     */
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        log.debug("Cancelling order: {} for user: {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("注文が見つかりません"));

        if (!order.isCancellable()) {
            throw new IllegalStateException("この注文はキャンセルできません");
        }

        // 在庫を復元
        restoreBookStock(order);

        // 注文をキャンセル状態に更新
        order.updateStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order cancelled successfully: {} for user: {}", orderId, userId);
    }

    // プライベートメソッド

    /**
     * カートから注文を作成
     */
    private Order createOrderFromCart(Cart cart, CreateOrderRequest request) {
        User user = cart.getUser();

        Order order = Order.builder()
                .user(user)
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .shippingPhone(request.getShippingPhone())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        // カートアイテムを注文アイテムに変換
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> createOrderItemFromCartItem(cartItem, order))
                .collect(Collectors.toList());

        orderItems.forEach(order::addItem);
        order.recalculateAmounts();

        return order;
    }

    /**
     * カートアイテムから注文アイテムを作成
     */
    private OrderItem createOrderItemFromCartItem(CartItem cartItem, Order order) {
        Book book = cartItem.getBook();
        
        return OrderItem.builder()
                .order(order)
                .book(book)
                .bookTitle(book.getTitle())
                .bookAuthor(book.getAuthor())
                .bookIsbn(book.getIsbn())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .subtotal(cartItem.getSubtotal())
                .build();
    }

    /**
     * 在庫チェックと予約
     */
    private void validateAndReserveStock(Cart cart) {
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            Integer requiredQuantity = item.getQuantity();

            if (book.getStockQuantity() == null || book.getStockQuantity() < requiredQuantity) {
                throw new IllegalStateException(
                    String.format("「%s」の在庫が不足しています。（必要: %d, 在庫: %d）", 
                        book.getTitle(), requiredQuantity, book.getStockQuantity())
                );
            }
        }
    }

    /**
     * 書籍在庫を更新（減算）
     * パフォーマンス向上のためバッチで処理
     */
    private void updateBookStock(Cart cart) {
        List<Book> booksToUpdate = new ArrayList<>();
        
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            Integer oldStock = book.getStockQuantity();
            Integer newStock = book.getStockQuantity() - item.getQuantity();
            book.setStockQuantity(Math.max(0, newStock));
            booksToUpdate.add(book);
            
            log.debug("Prepared stock update for book {}: {} -> {}", 
                book.getId(), oldStock, newStock);
        }
        
        // バッチで一括保存
        if (!booksToUpdate.isEmpty()) {
            bookRepository.saveAll(booksToUpdate);
        }
    }

    /**
     * 書籍在庫を復元（キャンセル時）
     */
    private void restoreBookStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            if (book != null) {
                Integer restoredStock = book.getStockQuantity() + item.getQuantity();
                book.setStockQuantity(restoredStock);
                bookRepository.save(book);
                
                log.debug("Restored stock for book {}: {} -> {}", 
                    book.getId(), book.getStockQuantity() - item.getQuantity(), restoredStock);
            }
        }
    }

    /**
     * 注文統計を取得
     */
    public OrderStatistics getOrderStatistics(Long userId) {
        Long orderCount = orderRepository.countOrdersByUserId(userId);
        Double totalAmount = orderRepository.getTotalAmountByUserId(userId);
        
        return new OrderStatistics(orderCount, BigDecimal.valueOf(totalAmount != null ? totalAmount : 0.0));
    }

    // 内部クラス
    public static class OrderStatistics {
        private final Long orderCount;
        private final BigDecimal totalAmount;

        public OrderStatistics(Long orderCount, BigDecimal totalAmount) {
            this.orderCount = orderCount;
            this.totalAmount = totalAmount;
        }

        public Long getOrderCount() { return orderCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getFormattedTotalAmount() {
            return String.format("¥%,.0f", totalAmount);
        }
    }
}