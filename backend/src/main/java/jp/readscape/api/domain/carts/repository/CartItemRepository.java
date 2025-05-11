package jp.readscape.api.domain.carts.repository;

import jp.readscape.api.domain.carts.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // カスタムクエリ例：
    List<CartItem> findByTitleContainingOrAuthorContaining(String titleKeyword, String authorKeyword);
    List<CartItem> findByCategory(String category);

}
