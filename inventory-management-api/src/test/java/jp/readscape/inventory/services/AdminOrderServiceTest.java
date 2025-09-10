package jp.readscape.inventory.services;

import jp.readscape.inventory.domain.orders.model.Order;
import jp.readscape.inventory.domain.orders.model.OrderItem;
import jp.readscape.inventory.domain.orders.repository.OrderRepository;
import jp.readscape.inventory.domain.users.model.User;
import jp.readscape.inventory.domain.books.model.Book;
import jp.readscape.inventory.dto.admin.*;
import jp.readscape.inventory.exceptions.OrderNotFoundException;
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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DtoMappingService dtoMappingService;

    @InjectMocks
    private AdminOrderService adminOrderService;

    private Order testOrder;
    private User testUser;
    private Book testBook;
    private OrderItem testOrderItem;
    private AdminOrderView testAdminOrderView;
    private AdminOrderDetail testAdminOrderDetail;
    private PendingOrder testPendingOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser@example.com")
                .email("testuser@example.com")
                .displayName("Test User")
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("9781234567890")
                .price(BigDecimal.valueOf(1500))
                .build();

        testOrderItem = OrderItem.builder()
                .id(1L)
                .book(testBook)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(1500))
                .subtotal(BigDecimal.valueOf(3000))
                .build();

        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-20231201-0001")
                .user(testUser)
                .status(Order.OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(3000))
                .shippingAddress("Test Address")
                .orderItems(Arrays.asList(testOrderItem))
                .build();

        testAdminOrderView = AdminOrderView.builder()
                .orderId(1L)
                .orderNumber("ORD-20231201-0001")
                .customerName("Test User")
                .status("PENDING")
                .totalAmount(BigDecimal.valueOf(3000))
                .orderDate(LocalDateTime.now())
                .build();

        testAdminOrderDetail = AdminOrderDetail.builder()
                .orderId(1L)
                .orderNumber("ORD-20231201-0001")
                .customerName("Test User")
                .customerEmail("testuser@example.com")
                .status("PENDING")
                .totalAmount(BigDecimal.valueOf(3000))
                .orderDate(LocalDateTime.now())
                .shippingAddress("Test Address")
                .build();

        testPendingOrder = PendingOrder.builder()
                .orderId(1L)
                .orderNumber("ORD-20231201-0001")
                .customerName("Test User")
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(3000))
                .build();
    }

    @Test
    void getOrders_WithNoStatusFilter_ShouldReturnAllOrders() {
        // Given
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderDate"));
        
        when(orderRepository.findAll(expectedPageable)).thenReturn(orderPage);
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        Page<AdminOrderView> result = adminOrderService.getOrders(null, 0, 10, "orderDate", "desc");

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testAdminOrderView);
        verify(orderRepository).findAll(expectedPageable);
    }

    @Test
    void getOrders_WithValidStatusFilter_ShouldReturnFilteredOrders() {
        // Given
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "orderDate"));
        
        when(orderRepository.findByStatus(Order.OrderStatus.PENDING, expectedPageable)).thenReturn(orderPage);
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        Page<AdminOrderView> result = adminOrderService.getOrders("PENDING", 0, 10, "orderDate", "asc");

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByStatus(Order.OrderStatus.PENDING, expectedPageable);
    }

    @Test
    void getOrders_WithInvalidStatusFilter_ShouldReturnAllOrders() {
        // Given
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderDate"));
        
        when(orderRepository.findAll(expectedPageable)).thenReturn(orderPage);
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        Page<AdminOrderView> result = adminOrderService.getOrders("INVALID_STATUS", 0, 10, "orderDate", "desc");

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(expectedPageable);
    }

    @Test
    void getOrders_BackwardCompatibility_ShouldUseDefaultSort() {
        // Given
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderDate"));
        
        when(orderRepository.findAll(expectedPageable)).thenReturn(orderPage);
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        Page<AdminOrderView> result = adminOrderService.getOrders(null, 0, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(expectedPageable);
    }

    @Test
    void getOrderById_WithExistingOrder_ShouldReturnOrderDetail() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(dtoMappingService.mapToAdminOrderDetail(testOrder)).thenReturn(testAdminOrderDetail);

        // When
        AdminOrderDetail result = adminOrderService.getOrderById(1L);

        // Then
        assertThat(result).isEqualTo(testAdminOrderDetail);
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_WithNonExistentOrder_ShouldThrowOrderNotFoundException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminOrderService.getOrderById(1L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("注文が見つかりません: 1");
    }

    @Test
    void getOrderDetail_BackwardCompatibility_ShouldCallGetOrderById() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(dtoMappingService.mapToAdminOrderDetail(testOrder)).thenReturn(testAdminOrderDetail);

        // When
        AdminOrderDetail result = adminOrderService.getOrderDetail(1L);

        // Then
        assertThat(result).isEqualTo(testAdminOrderDetail);
        verify(orderRepository).findById(1L);
    }

    @Test
    void updateOrderStatus_WithValidTransition_ShouldUpdateStatus() {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus("CONFIRMED");
        request.setReason("Order confirmed by admin");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When
        adminOrderService.updateOrderStatus(1L, request);

        // Then
        assertThat(testOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        assertThat(testOrder.getNotes()).isEqualTo("Order confirmed by admin");
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_WithInvalidTransition_ShouldThrowException() {
        // Given
        testOrder.setStatus(Order.OrderStatus.DELIVERED);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("完了またはキャンセルされた注文のステータスは変更できません");
    }

    @Test
    void updateOrderStatus_WithNonExistentOrder_ShouldThrowOrderNotFoundException() {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus("CONFIRMED");

        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getPendingOrders_WithLimit_ShouldReturnLimitedPendingOrders() {
        // Given
        Page<Order> pendingOrderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 5, Sort.by("orderDate").ascending());

        when(orderRepository.findByStatusIn(
                eq(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED)), 
                eq(expectedPageable))).thenReturn(pendingOrderPage);
        when(dtoMappingService.mapToPendingOrder(testOrder)).thenReturn(testPendingOrder);

        // When
        List<PendingOrder> result = adminOrderService.getPendingOrders(5);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testPendingOrder);
    }

    @Test
    void getPendingOrders_WithoutLimit_ShouldUseDefaultLimit() {
        // Given
        Page<Order> pendingOrderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 100, Sort.by("orderDate").ascending());

        when(orderRepository.findByStatusIn(
                eq(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED)), 
                eq(expectedPageable))).thenReturn(pendingOrderPage);
        when(dtoMappingService.mapToPendingOrder(testOrder)).thenReturn(testPendingOrder);

        // When
        List<PendingOrder> result = adminOrderService.getPendingOrders();

        // Then
        assertThat(result).hasSize(1);
        verify(orderRepository).findByStatusIn(
                eq(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED)), 
                eq(expectedPageable));
    }

    @Test
    void getConfirmedOrders_ShouldReturnConfirmedOrders() {
        // Given
        when(orderRepository.findConfirmedOrders()).thenReturn(Arrays.asList(testOrder));
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        List<AdminOrderView> result = adminOrderService.getConfirmedOrders();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAdminOrderView);
    }

    @Test
    void searchOrders_WithQuery_ShouldReturnSearchResults() {
        // Given
        String query = "ORD-20231201";
        Page<Order> searchResults = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("orderDate").descending());

        when(orderRepository.findByOrderNumberContainingIgnoreCaseOrUser_UsernameContainingIgnoreCaseOrUser_EmailContainingIgnoreCase(
                eq(query), eq(query), eq(query), eq(expectedPageable))).thenReturn(searchResults);
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        Page<AdminOrderView> result = adminOrderService.searchOrders(query, 0, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testAdminOrderView);
    }

    @Test
    void getOrdersByDateRange_WithStatusFilter_ShouldReturnFilteredOrders() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        String status = "PENDING";
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("orderDate").descending());

        when(orderRepository.findByStatusAndOrderDateBetween(
                Order.OrderStatus.PENDING, startDate, endDate, expectedPageable))
                .thenReturn(orderPage);
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        Page<AdminOrderView> result = adminOrderService.getOrdersByDateRange(
                startDate, endDate, status, 0, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByStatusAndOrderDateBetween(
                Order.OrderStatus.PENDING, startDate, endDate, expectedPageable);
    }

    @Test
    void getOrdersByDateRange_WithoutStatusFilter_ShouldReturnAllOrdersInRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("orderDate").descending());

        when(orderRepository.findByOrderDateBetween(startDate, endDate, expectedPageable))
                .thenReturn(orderPage);
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        Page<AdminOrderView> result = adminOrderService.getOrdersByDateRange(
                startDate, endDate, null, 0, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByOrderDateBetween(startDate, endDate, expectedPageable);
    }

    @Test
    void getDelayedOrders_ShouldReturnDelayedOrders() {
        // Given
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(3);
        when(orderRepository.findDelayedOrders(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testOrder));
        when(dtoMappingService.mapToAdminOrderView(testOrder)).thenReturn(testAdminOrderView);

        // When
        List<AdminOrderView> result = adminOrderService.getDelayedOrders();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAdminOrderView);
        verify(orderRepository).findDelayedOrders(any(LocalDateTime.class));
    }

    @Test
    void cancelOrder_WithCancellableOrder_ShouldCancelOrder() {
        // Given
        String reason = "Customer requested cancellation";
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When
        adminOrderService.cancelOrder(1L, reason);

        // Then
        assertThat(testOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        assertThat(testOrder.getNotes()).contains("[キャンセル理由] " + reason);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void cancelOrder_WithNonCancellableOrder_ShouldThrowException() {
        // Given
        testOrder.setStatus(Order.OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> adminOrderService.cancelOrder(1L, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("この注文はキャンセルできません");
    }

    @Test
    void getOrderStatistics_WithDays_ShouldReturnStatistics() {
        // Given
        int days = 7;
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        when(orderRepository.findByOrderDateBetween(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        AdminOrderService.OrderStatistics result = adminOrderService.getOrderStatistics(days);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalOrders()).isEqualTo(1L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(result.getAverageAmount()).isEqualTo(3000.0);
    }

    @Test
    void getOrderStatistics_WithDateRange_ShouldReturnStatistics() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        when(orderRepository.findByOrderDateBetween(
                eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        AdminOrderService.OrderStatistics result = adminOrderService.getOrderStatistics(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStartDate()).isEqualTo(startDate);
        assertThat(result.getEndDate()).isEqualTo(endDate);
        assertThat(result.getTotalOrders()).isEqualTo(1L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(result.getStatusBreakdown()).containsEntry(Order.OrderStatus.PENDING, 1L);
    }

    @Test
    void getShippingLabelData_WithValidOrder_ShouldReturnShippingData() {
        // Given
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        AdminOrderService.ShippingLabelData result = adminOrderService.getShippingLabelData(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderNumber()).isEqualTo("ORD-20231201-0001");
        assertThat(result.getCustomerName()).isEqualTo("testuser@example.com");
        assertThat(result.getShippingAddress()).isEqualTo("Test Address");
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void getShippingLabelData_WithInvalidOrderStatus_ShouldThrowException() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> adminOrderService.getShippingLabelData(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("配送ラベルを生成できない注文ステータスです");
    }

    @Test
    void getTodayOrderStatistics_ShouldReturnTodayStats() {
        // Given
        Long todayOrderCount = 5L;
        Object[] todayStats = {5L, 15000.0};
        
        when(orderRepository.countTodaysOrders()).thenReturn(todayOrderCount);
        when(orderRepository.getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(todayStats);

        // When
        AdminOrderService.TodayOrderStatistics result = adminOrderService.getTodayOrderStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTodayOrderCount()).isEqualTo(5L);
        assertThat(result.getTodayTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(15000.0));
    }

    @Test
    void validateStatusTransition_FromPendingToConfirmed_ShouldAllow() {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus("CONFIRMED");
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When & Then - should not throw exception
        assertThatCode(() -> adminOrderService.updateOrderStatus(1L, request))
                .doesNotThrowAnyException();
    }

    @Test
    void validateStatusTransition_FromPendingToShipped_ShouldThrowException() {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus("SHIPPED");
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING状態からは CONFIRMED または CANCELLED にのみ変更可能です");
    }

    @Test
    void updateOrderStatus_WithLegacyMethod_ShouldWork() {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus("CONFIRMED");
        Long userId = 1L;

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When
        adminOrderService.updateOrderStatus(1L, request, userId);

        // Then
        assertThat(testOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void getOrderStatistics_WithEmptyResults_ShouldHandleGracefully() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        Page<Order> emptyPage = new PageImpl<>(Arrays.asList());
        when(orderRepository.findByOrderDateBetween(
                eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        AdminOrderService.OrderStatistics result = adminOrderService.getOrderStatistics(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalOrders()).isEqualTo(0L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getAverageAmount()).isEqualTo(0.0);
        assertThat(result.getStatusBreakdown()).isEmpty();
    }
}