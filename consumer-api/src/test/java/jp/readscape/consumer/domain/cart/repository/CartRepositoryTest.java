package jp.readscape.consumer.domain.cart.repository;

import jp.readscape.consumer.domain.books.model.Book;
import jp.readscape.consumer.domain.cart.model.Cart;
import jp.readscape.consumer.domain.cart.model.CartItem;
import jp.readscape.consumer.domain.users.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class CartRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    private User testUser1;
    private User testUser2;
    private Book testBook1;
    private Book testBook2;
    private Cart cartWithItems;
    private Cart emptyCart;
    private Cart oldCart;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = User.builder()
                .username("test1@example.com")
                .email("test1@example.com")
                .displayName("Test User 1")
                .build();

        testUser2 = User.builder()
                .username("test2@example.com")
                .email("test2@example.com")
                .displayName("Test User 2")
                .build();

        entityManager.persist(testUser1);
        entityManager.persist(testUser2);

        // Create test books
        testBook1 = Book.builder()
                .title("Test Book 1")
                .author("Test Author 1")
                .isbn("1111111111111")
                .price(BigDecimal.valueOf(1500))
                .category("技術書")
                .stockQuantity(10)
                .averageRating(BigDecimal.valueOf(4.5))
                .reviewCount(10)
                .build();

        testBook2 = Book.builder()
                .title("Test Book 2")
                .author("Test Author 2")
                .isbn("2222222222222")
                .price(BigDecimal.valueOf(2000))
                .category("小説")
                .stockQuantity(5)
                .averageRating(BigDecimal.valueOf(4.0))
                .reviewCount(5)
                .build();

        entityManager.persist(testBook1);
        entityManager.persist(testBook2);

        // Create cart with items
        cartWithItems = Cart.builder()
                .user(testUser1)
                .build();
        
        CartItem item1 = CartItem.builder()
                .cart(cartWithItems)
                .book(testBook1)
                .quantity(2)
                .unitPrice(testBook1.getPrice())
                .subtotal(testBook1.getPrice().multiply(BigDecimal.valueOf(2)))
                .build();

        CartItem item2 = CartItem.builder()
                .cart(cartWithItems)
                .book(testBook2)
                .quantity(1)
                .unitPrice(testBook2.getPrice())
                .subtotal(testBook2.getPrice())
                .build();

        cartWithItems.getItems().add(item1);
        cartWithItems.getItems().add(item2);
        cartWithItems.setUpdatedAt(LocalDateTime.now());

        entityManager.persist(cartWithItems);
        entityManager.persist(item1);
        entityManager.persist(item2);

        // Create empty cart
        emptyCart = Cart.builder()
                .user(testUser2)
                .build();
        emptyCart.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(emptyCart);

        // Create old cart (for testing time-based queries)
        oldCart = Cart.builder()
                .user(User.builder().username("old@example.com").email("old@example.com").build())
                .build();
        entityManager.persist(oldCart.getUser());
        oldCart.setUpdatedAt(LocalDateTime.now().minusDays(30));
        entityManager.persist(oldCart);

        entityManager.flush();
    }

    @Test
    void findByUserId_WithExistingUser_ShouldReturnCart() {
        // When
        Optional<Cart> result = cartRepository.findByUserId(testUser1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(testUser1.getId());
        assertThat(result.get().getItems()).hasSize(2);
    }

    @Test
    void findByUserId_WithNonExistentUser_ShouldReturnEmpty() {
        // When
        Optional<Cart> result = cartRepository.findByUserId(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByUser_WithExistingUser_ShouldReturnCart() {
        // When
        Optional<Cart> result = cartRepository.findByUser(testUser1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(testUser1.getId());
        assertThat(result.get().getItems()).hasSize(2);
    }

    @Test
    void findByUser_WithUserWithEmptyCart_ShouldReturnEmptyCart() {
        // When
        Optional<Cart> result = cartRepository.findByUser(testUser2);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(testUser2.getId());
        assertThat(result.get().getItems()).isEmpty();
    }

    @Test
    void existsByUserId_WithExistingUser_ShouldReturnTrue() {
        // When
        boolean exists = cartRepository.existsByUserId(testUser1.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserId_WithNonExistentUser_ShouldReturnFalse() {
        // When
        boolean exists = cartRepository.existsByUserId(999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findEmptyCarts_ShouldReturnCartsWithNoItems() {
        // When
        List<Cart> result = cartRepository.findEmptyCarts();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).extracting(Cart::getItems)
                .allMatch(items -> items.isEmpty());
        
        // Should include emptyCart and oldCart
        assertThat(result).hasSize(2);
    }

    @Test
    void findByUpdatedAtBefore_WithRecentDateTime_ShouldReturnOldCarts() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);

        // When
        List<Cart> result = cartRepository.findByUpdatedAtBefore(cutoffTime);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).extracting(Cart::getUpdatedAt)
                .allMatch(updatedAt -> updatedAt.isBefore(cutoffTime));
        
        // Should include only oldCart
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(oldCart);
    }

    @Test
    void findByUpdatedAtBefore_WithVeryOldDateTime_ShouldReturnEmpty() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusYears(1);

        // When
        List<Cart> result = cartRepository.findByUpdatedAtBefore(cutoffTime);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findCartsWithMinimumItems_WithLowMinimum_ShouldReturnCartsWithItems() {
        // Given
        int minItems = 1;

        // When
        List<Cart> result = cartRepository.findCartsWithMinimumItems(minItems);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(cart -> cart.getItems().size() >= minItems);
        
        // Should include cartWithItems (2 items)
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(cartWithItems);
    }

    @Test
    void findCartsWithMinimumItems_WithHighMinimum_ShouldReturnLimitedResults() {
        // Given
        int minItems = 3;

        // When
        List<Cart> result = cartRepository.findCartsWithMinimumItems(minItems);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findCartsWithMinimumItems_WithExactMatch_ShouldReturnMatchingCarts() {
        // Given
        int minItems = 2;

        // When
        List<Cart> result = cartRepository.findCartsWithMinimumItems(minItems);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItems()).hasSize(2);
    }

    @Test
    void findCartsByBookId_WithExistingBook_ShouldReturnCartsContainingBook() {
        // When
        List<Cart> result = cartRepository.findCartsByBookId(testBook1.getId());

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(cartWithItems);
        
        // Verify the cart actually contains the book
        assertThat(result.get(0).getItems())
                .anyMatch(item -> item.getBook().getId().equals(testBook1.getId()));
    }

    @Test
    void findCartsByBookId_WithBookInMultipleCarts_ShouldReturnAllCarts() {
        // Given - create another cart with the same book
        User anotherUser = User.builder()
                .username("another@example.com")
                .email("another@example.com")
                .displayName("Another User")
                .build();
        entityManager.persist(anotherUser);

        Cart anotherCart = Cart.builder()
                .user(anotherUser)
                .build();

        CartItem anotherItem = CartItem.builder()
                .cart(anotherCart)
                .book(testBook1)
                .quantity(1)
                .unitPrice(testBook1.getPrice())
                .subtotal(testBook1.getPrice())
                .build();

        anotherCart.getItems().add(anotherItem);
        entityManager.persist(anotherCart);
        entityManager.persist(anotherItem);
        entityManager.flush();

        // When
        List<Cart> result = cartRepository.findCartsByBookId(testBook1.getId());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(cart -> cart.getUser().getId())
                .containsExactlyInAnyOrder(testUser1.getId(), anotherUser.getId());
    }

    @Test
    void findCartsByBookId_WithNonExistentBook_ShouldReturnEmpty() {
        // When
        List<Cart> result = cartRepository.findCartsByBookId(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getTotalItemCountByUserId_WithItemsInCart_ShouldReturnCorrectCount() {
        // When
        Integer result = cartRepository.getTotalItemCountByUserId(testUser1.getId());

        // Then
        assertThat(result).isEqualTo(3); // 2 + 1 quantities from the two items
    }

    @Test
    void getTotalItemCountByUserId_WithEmptyCart_ShouldReturnZero() {
        // When
        Integer result = cartRepository.getTotalItemCountByUserId(testUser2.getId());

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    void getTotalItemCountByUserId_WithNonExistentUser_ShouldReturnZero() {
        // When
        Integer result = cartRepository.getTotalItemCountByUserId(999L);

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    void deleteByUpdatedAtBeforeAndItemsIsEmpty_ShouldDeleteOldEmptyCarts() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);

        // Verify we have old empty cart before deletion
        List<Cart> oldEmptyCarts = cartRepository.findByUpdatedAtBefore(cutoffTime);
        assertThat(oldEmptyCarts).hasSize(1);
        assertThat(oldEmptyCarts.get(0).getItems()).isEmpty();

        // When
        cartRepository.deleteByUpdatedAtBeforeAndItemsIsEmpty(cutoffTime);
        entityManager.flush();

        // Then
        List<Cart> remainingOldCarts = cartRepository.findByUpdatedAtBefore(cutoffTime);
        assertThat(remainingOldCarts).isEmpty();

        // Verify recent carts are not affected
        Optional<Cart> recentCart = cartRepository.findById(cartWithItems.getId());
        assertThat(recentCart).isPresent();
        
        Optional<Cart> recentEmptyCart = cartRepository.findById(emptyCart.getId());
        assertThat(recentEmptyCart).isPresent();
    }

    @Test
    void basicCrudOperations_ShouldWork() {
        // Create new user and cart
        User newUser = User.builder()
                .username("new@example.com")
                .email("new@example.com")
                .displayName("New User")
                .build();
        entityManager.persist(newUser);

        Cart newCart = Cart.builder()
                .user(newUser)
                .build();

        // Test save
        Cart saved = cartRepository.save(newCart);
        assertThat(saved.getId()).isNotNull();

        // Test findById
        Optional<Cart> found = cartRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(newUser.getId());

        // Test delete
        cartRepository.delete(saved);
        entityManager.flush();
        
        Optional<Cart> deleted = cartRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllCarts() {
        // When
        List<Cart> result = cartRepository.findAll();

        // Then
        assertThat(result).hasSize(3); // cartWithItems, emptyCart, oldCart
    }

    @Test
    void cartWithMultipleItems_ShouldMaintainItemIntegrity() {
        // Given
        Optional<Cart> cart = cartRepository.findByUserId(testUser1.getId());
        assertThat(cart).isPresent();

        // When
        Cart foundCart = cart.get();

        // Then
        assertThat(foundCart.getItems()).hasSize(2);
        
        // Verify item details
        CartItem firstItem = foundCart.getItems().stream()
                .filter(item -> item.getBook().getId().equals(testBook1.getId()))
                .findFirst()
                .orElseThrow();
        
        assertThat(firstItem.getQuantity()).isEqualTo(2);
        assertThat(firstItem.getUnitPrice()).isEqualByComparingTo(testBook1.getPrice());
        assertThat(firstItem.getSubtotal())
                .isEqualByComparingTo(testBook1.getPrice().multiply(BigDecimal.valueOf(2)));

        CartItem secondItem = foundCart.getItems().stream()
                .filter(item -> item.getBook().getId().equals(testBook2.getId()))
                .findFirst()
                .orElseThrow();
        
        assertThat(secondItem.getQuantity()).isEqualTo(1);
        assertThat(secondItem.getUnitPrice()).isEqualByComparingTo(testBook2.getPrice());
        assertThat(secondItem.getSubtotal()).isEqualByComparingTo(testBook2.getPrice());
    }
}