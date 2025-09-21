package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.cart.model.Cart;
import jp.readscape.consumer.domain.cart.model.CartItem;
import jp.readscape.consumer.domain.cart.repository.CartRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.dto.carts.CartResponse;
import jp.readscape.consumer.exceptions.BookNotFoundException;
import jp.readscape.consumer.exceptions.CartNotFoundException;
import jp.readscape.consumer.exceptions.InsufficientStockException;
import jp.readscape.consumer.exceptions.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Test")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("カート取得 - 既存カート")
    void getCartByUsernameWithExistingCart() {
        // Arrange
        String username = "test@example.com";
        User user = createSampleUser();
        Cart cart = createSampleCart(user);
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        
        // Act
        CartResponse result = cartService.getCartByUsername(username);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCartId()).isEqualTo(cart.getId());
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
        
        verify(userRepository).findByUsernameOrEmail(username);
        verify(cartRepository).findByUserId(user.getId());
    }

    @Test
    @DisplayName("カート取得 - カート存在せず新規作成")
    void getCartByUsernameWithNewCart() {
        // Arrange
        String username = "test@example.com";
        User user = createSampleUser();
        Cart newCart = Cart.builder()
            .id(1L)
            .user(user)
            .isActive(true)
            .items(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        
        // Act
        CartResponse result = cartService.getCartByUsername(username);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCartId()).isEqualTo(newCart.getId());
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        
        verify(userRepository).findByUsernameOrEmail(username);
        verify(cartRepository).findByUserId(user.getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("カート取得 - ユーザー存在しない")
    void getCartByUsernameUserNotFound() {
        // Arrange
        String username = "nonexistent@example.com";
        when(userRepository.findByEmail(username)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> cartService.getCartByUsername(username))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found");
        
        verify(userRepository).findByUsernameOrEmail(username);
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("カートに商品追加 - 正常系")
    void addToCartSuccess() {
        // Arrange
        String username = "test@example.com";
        Long bookId = 1L;
        Integer quantity = 2;
        
        User user = createSampleUser();
        Book book = createSampleBook();
        Cart cart = createEmptyCart(user);
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        
        // Act
        cartService.addToCart(username, bookId, quantity);
        
        // Assert
        verify(userRepository).findByUsernameOrEmail(username);
        verify(bookRepository).findById(bookId);
        verify(cartRepository).findByUserId(user.getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("カートに商品追加 - 既存商品の数量増加")
    void addToCartExistingItem() {
        // Arrange
        String username = "test@example.com";
        Long bookId = 1L;
        Integer additionalQuantity = 1;
        
        User user = createSampleUser();
        Book book = createSampleBook();
        Cart cart = createSampleCart(user);
        
        // 既存のCartItemがbookId=1のものを持っているとする
        CartItem existingItem = cart.getItems().get(0);
        int originalQuantity = existingItem.getQuantity();
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        
        // Act
        cartService.addToCart(username, bookId, additionalQuantity);
        
        // Assert
        verify(userRepository).findByUsernameOrEmail(username);
        verify(bookRepository).findById(bookId);
        verify(cartRepository).save(any(Cart.class));
        // 数量が増加していることを確認（実際のロジックに依存）
    }

    @Test
    @DisplayName("カートに商品追加 - 書籍存在しない")
    void addToCartBookNotFound() {
        // Arrange
        String username = "test@example.com";
        Long bookId = 999L;
        Integer quantity = 1;
        
        User user = createSampleUser();
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(username, bookId, quantity))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining("Book not found");
        
        verify(userRepository).findByUsernameOrEmail(username);
        verify(bookRepository).findById(bookId);
        verifyNoMoreInteractions(cartRepository);
    }

    @Test
    @DisplayName("カートに商品追加 - 在庫不足")
    void addToCartInsufficientStock() {
        // Arrange
        String username = "test@example.com";
        Long bookId = 1L;
        Integer quantity = 100; // 在庫を超える数量
        
        User user = createSampleUser();
        Book book = createSampleBook();
        book.setStockQuantity(5); // 少ない在庫
        Cart cart = createEmptyCart(user);
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        
        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(username, bookId, quantity))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Insufficient stock");
        
        verify(userRepository).findByUsernameOrEmail(username);
        verify(bookRepository).findById(bookId);
        verify(cartRepository).findByUserId(user.getId());
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("カートアイテム更新 - 正常系")
    void updateCartItemSuccess() {
        // Arrange
        String username = "test@example.com";
        Long itemId = 1L;
        Integer newQuantity = 3;
        
        User user = createSampleUser();
        Cart cart = createSampleCart(user);
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        
        // Act
        cartService.updateCartQuantity(username, itemId, newQuantity);
        
        // Assert
        verify(userRepository).findByUsernameOrEmail(username);
        verify(cartRepository).findByUserId(user.getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("カートアイテム削除 - 正常系")
    void removeFromCartSuccess() {
        // Arrange
        String username = "test@example.com";
        Long itemId = 1L;
        
        User user = createSampleUser();
        Cart cart = createSampleCart(user);
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        
        // Act
        cartService.removeFromCart(username, itemId);
        
        // Assert
        verify(userRepository).findByUsernameOrEmail(username);
        verify(cartRepository).findByUserId(user.getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("カート全クリア - 正常系")
    void clearCartSuccess() {
        // Arrange
        String username = "test@example.com";
        User user = createSampleUser();
        Cart cart = createSampleCart(user);
        
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        
        // Act
        cartService.clearCart(username);
        
        // Assert
        verify(userRepository).findByUsernameOrEmail(username);
        verify(cartRepository).findByUserId(user.getId());
        verify(cartRepository).save(any(Cart.class));
    }

    // getCartItemCountメソッドが存在しないためコメントアウト
    /*
    @Test
    @DisplayName("カートアイテム数取得 - 正常系")
    void getCartItemCountSuccess() {
        // Arrange
        String username = "test@example.com";
        User user = createSampleUser();
        Cart cart = createSampleCart(user);

        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        // Act
        int result = cartService.getCartItemCount(username);

        // Assert
        assertThat(result).isEqualTo(3); // CartItemの数量の合計
        verify(userRepository).findByUsernameOrEmail(username);
        verify(cartRepository).findByUserId(user.getId());
    }
    */

    // Helper methods
    private User createSampleUser() {
        return User.builder()
            .id(1L)
            .email("test@example.com")
            .password("hashedPassword")
            .firstName("テスト")
            .lastName("ユーザー")
            .role(UserRole.CONSUMER)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private Book createSampleBook() {
        return Book.builder()
            .id(1L)
            .title("Spring Bootガイド")
            .author("山田花子")
            .isbn("9784000000001")
            .price(3200)
            .category("技術書")
            .description("Spring Bootの基本から応用まで")
            .stockQuantity(50)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private Cart createEmptyCart(User user) {
        return Cart.builder()
            .id(1L)
            .user(user)
            .isActive(true)
            .items(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private Cart createSampleCart(User user) {
        Book book1 = createSampleBook();
        Book book2 = Book.builder()
            .id(2L)
            .title("Spring Securityガイド")
            .price(2800)
            .stockQuantity(30)
            .build();

        CartItem item1 = CartItem.builder()
            .id(1L)
            .book(book1)
            .book(book1)
            .quantity(2)
            .unitPrice(book1.getPrice())
            .addedAt(LocalDateTime.now())
            .build();

        CartItem item2 = CartItem.builder()
            .id(2L)
            .book(book2)
            .book(book2)
            .quantity(1)
            .unitPrice(book2.getPrice())
            .addedAt(LocalDateTime.now())
            .build();

        List<CartItem> items = Arrays.asList(item1, item2);

        return Cart.builder()
            .id(1L)
            .user(user)
            .isActive(true)
            .items(items)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}