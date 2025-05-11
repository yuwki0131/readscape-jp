package jp.readscape.api.domain.orders.repository;

import jp.readscape.api.domain.orders.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // カスタムクエリ例：
    List<Order> findByTitleContainingOrAuthorContaining(String titleKeyword, String authorKeyword);
    List<Order> findByCategory(String category);

}
