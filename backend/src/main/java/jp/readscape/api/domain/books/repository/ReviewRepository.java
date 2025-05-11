package jp.readscape.api.domain.books.repository;

import jp.readscape.api.domain.books.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // カスタムクエリ例：
    List<Review> findByTitleContainingOrAuthorContaining(String titleKeyword, String authorKeyword);
    List<Review> findByCategory(String category);

}
