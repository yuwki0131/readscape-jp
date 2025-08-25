package jp.readscape.consumer.domain.cart.repository;

import jp.readscape.consumer.domain.cart.model.Cart;
import jp.readscape.consumer.domain.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * ユーザーIDでカートを検索
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * ユーザーでカートを検索
     */
    Optional<Cart> findByUser(User user);

    /**
     * ユーザーのカートの存在チェック
     */
    boolean existsByUserId(Long userId);

    /**
     * 空のカートを検索
     */
    @Query("SELECT c FROM Cart c WHERE c.items IS EMPTY")
    List<Cart> findEmptyCarts();

    /**
     * 最終更新日時が指定日時より古いカートを検索
     */
    List<Cart> findByUpdatedAtBefore(LocalDateTime dateTime);

    /**
     * アイテム数が指定値以上のカートを検索
     */
    @Query("SELECT c FROM Cart c WHERE SIZE(c.items) >= :minItems")
    List<Cart> findCartsWithMinimumItems(@Param("minItems") int minItems);

    /**
     * 特定の書籍を含むカートを検索
     */
    @Query("SELECT c FROM Cart c JOIN c.items ci WHERE ci.book.id = :bookId")
    List<Cart> findCartsByBookId(@Param("bookId") Long bookId);

    /**
     * ユーザーのカート内アイテム数を取得
     */
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM Cart c JOIN c.items ci WHERE c.user.id = :userId")
    Integer getTotalItemCountByUserId(@Param("userId") Long userId);

    /**
     * 古いカートを削除
     */
    void deleteByUpdatedAtBeforeAndItemsIsEmpty(LocalDateTime dateTime);
}