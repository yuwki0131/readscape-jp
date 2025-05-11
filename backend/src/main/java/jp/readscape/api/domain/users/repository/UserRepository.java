package jp.readscape.api.domain.users.repository;

import jp.readscape.api.domain.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {

    // カスタムクエリ例：
    List<User> findByTitleContainingOrAuthorContaining(String titleKeyword, String authorKeyword);
    List<User> findByCategory(String category);

}
