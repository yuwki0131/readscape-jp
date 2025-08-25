package jp.readscape.consumer.domain.users.repository;

import jp.readscape.consumer.domain.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ユーザー名による検索
     */
    Optional<User> findByUsername(String username);

    /**
     * メールアドレスによる検索
     */
    Optional<User> findByEmail(String email);

    /**
     * ユーザー名またはメールアドレスによる検索
     */
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * ユーザー名の存在チェック
     */
    boolean existsByUsername(String username);

    /**
     * メールアドレスの存在チェック
     */
    boolean existsByEmail(String email);

    /**
     * アクティブなユーザーのみ検索
     */
    List<User> findByIsActiveTrue();

    /**
     * ロール別ユーザー検索
     */
    List<User> findByRole(User.UserRole role);

    /**
     * アクティブなユーザーをロール別で検索
     */
    List<User> findByRoleAndIsActiveTrue(User.UserRole role);

    /**
     * キーワード検索（ユーザー名、姓名、メールアドレス）
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> findByKeyword(@Param("keyword") String keyword);
}