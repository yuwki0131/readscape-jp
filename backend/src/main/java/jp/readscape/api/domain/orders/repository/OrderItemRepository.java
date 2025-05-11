package jp.readscape.api.domain.orders.repository;

import jp.readscape.api.domain.orders.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // カスタムクエリ例：
    List<OrderItem> findByTitleContainingOrAuthorContaining(String titleKeyword, String authorKeyword);
    List<OrderItem> findByCategory(String category);

}
