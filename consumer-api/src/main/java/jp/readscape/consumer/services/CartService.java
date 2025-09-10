package jp.readscape.consumer.services;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.books.repository.BookRepository;
import jp.readscape.consumer.domain.cart.model.Cart;
import jp.readscape.consumer.domain.cart.model.CartItem;
import jp.readscape.consumer.domain.cart.repository.CartRepository;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.repository.UserRepository;
import jp.readscape.consumer.dto.carts.CartItemResponse;
import jp.readscape.consumer.dto.carts.CartResponse;
import jp.readscape.consumer.exceptions.BookNotFoundException;
import jp.readscape.consumer.exceptions.CartNotFoundException;
import jp.readscape.consumer.exceptions.InsufficientStockException;
import jp.readscape.consumer.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * ユーザー名でカートを取得
     */
    @Transactional(readOnly = true)
    public CartResponse getCartByUsername(String username) {
        log.debug("Getting cart for user: {}", username);
        
        User user = findUserByUsername(username);
        Cart cart = findOrCreateCart(user);
        
        return buildCartResponse(cart);
    }

    /**
     * カートに商品を追加
     */
    public void addToCart(String username, Long bookId, Integer quantity) {
        log.debug("Adding book {} with quantity {} to cart for user: {}", bookId, quantity, username);
        
        User user = findUserByUsername(username);
        Book book = findBookById(bookId);
        Cart cart = findOrCreateCart(user);
        
        // 在庫チェック
        validateStock(book, quantity);
        
        // カートアイテムを作成・追加
        CartItem cartItem = CartItem.builder()
                .book(book)
                .quantity(quantity)
                .unitPrice(book.getPrice())
                .build();
        
        cart.addItem(cartItem);
        cartRepository.save(cart);
        
        log.info("Added book {} with quantity {} to cart for user: {}", bookId, quantity, username);
    }

    /**
     * カート内商品の数量を更新
     */
    public void updateCartQuantity(String username, Long bookId, Integer newQuantity) {
        log.debug("Updating cart quantity for book {} to {} for user: {}", bookId, newQuantity, username);
        
        User user = findUserByUsername(username);
        Cart cart = findCartByUser(user);
        
        // 書籍の在庫チェック
        Book book = findBookById(bookId);
        validateStock(book, newQuantity);
        
        // 数量更新
        cart.updateItemQuantity(bookId, newQuantity);
        cartRepository.save(cart);
        
        log.info("Updated cart quantity for book {} to {} for user: {}", bookId, newQuantity, username);
    }

    /**
     * カートから商品を削除
     */
    public void removeFromCart(String username, Long bookId) {
        log.debug("Removing book {} from cart for user: {}", bookId, username);
        
        User user = findUserByUsername(username);
        Cart cart = findCartByUser(user);
        
        cart.removeItem(bookId);
        cartRepository.save(cart);
        
        log.info("Removed book {} from cart for user: {}", bookId, username);
    }

    /**
     * カートを空にする
     */
    public void clearCart(String username) {
        log.debug("Clearing cart for user: {}", username);
        
        User user = findUserByUsername(username);
        Cart cart = findCartByUser(user);
        
        cart.clear();
        cartRepository.save(cart);
        
        log.info("Cleared cart for user: {}", username);
    }

    /**
     * ユーザーIDでカートを取得（注文処理で使用）
     */
    @Transactional(readOnly = true)
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("カートが見つかりません"));
    }

    // プライベートメソッド

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("ユーザーが見つかりません: " + username));
    }

    private Book findBookById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("書籍が見つかりません: " + bookId));
    }

    private Cart findCartByUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CartNotFoundException("カートが見つかりません"));
    }

    private Cart findOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.debug("Creating new cart for user: {}", user.getUsername());
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private void validateStock(Book book, Integer requestedQuantity) {
        if (book.getStockQuantity() == null || book.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                String.format("書籍「%s」の在庫が不足しています。要求数量: %d, 在庫数: %d", 
                    book.getTitle(), requestedQuantity, book.getStockQuantity())
            );
        }
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::buildCartItemResponse)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .totalItemCount(cart.getTotalItemCount())
                .totalAmount(cart.getTotalAmount())
                .formattedTotalAmount(cart.getFormattedTotalAmount())
                .isEmpty(cart.isEmpty())
                .build();
    }

    private CartItemResponse buildCartItemResponse(CartItem item) {
        Book book = item.getBook();
        
        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .bookAuthor(book.getAuthor())
                .bookImageUrl(book.getImageUrl())
                .unitPrice(item.getUnitPrice())
                .formattedUnitPrice(item.getFormattedUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .formattedSubtotal(item.getFormattedSubtotal())
                .stockQuantity(book.getStockQuantity())
                .stockAvailable(item.isQuantityAvailable())
                .build();
    }
}