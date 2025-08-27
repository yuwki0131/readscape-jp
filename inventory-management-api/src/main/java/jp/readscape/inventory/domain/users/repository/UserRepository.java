package jp.readscape.inventory.domain.users.repository;

import jp.readscape.inventory.domain.users.model.User;
import jp.readscape.inventory.domain.users.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ユーザーリポジトリ
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * メールアドレスでユーザーを検索
     */
    Optional<User> findByEmail(String email);

    /**
     * ユーザー名でユーザーを検索
     */
    Optional<User> findByUsername(String username);

    /**
     * メールアドレスまたはユーザー名でユーザーを検索
     */
    @Query("SELECT u FROM User u WHERE u.email = :emailOrUsername OR u.username = :emailOrUsername")
    Optional<User> findByEmailOrUsername(@Param("emailOrUsername") String emailOrUsername);

    /**
     * アクティブユーザーのみを取得
     */
    List<User> findByIsActiveTrue();

    /**
     * アクティブユーザーのページング取得
     */
    Page<User> findByIsActiveTrue(Pageable pageable);

    /**
     * ロールでユーザーを検索
     */
    List<User> findByRole(UserRole role);

    /**
     * ロールでアクティブユーザーを検索
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);

    /**
     * メールアドレスの存在確認
     */
    boolean existsByEmail(String email);

    /**
     * ユーザー名の存在確認
     */
    boolean existsByUsername(String username);

    /**
     * 管理者権限を持つユーザー一覧を取得
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('ADMIN', 'MANAGER') AND u.isActive = true")
    List<User> findActiveAdminUsers();

    /**
     * ユーザー名またはメールアドレスで部分一致検索
     */
    @Query("SELECT u FROM User u WHERE " +
           "(u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR " +
           " u.firstName LIKE %:keyword% OR u.lastName LIKE %:keyword%) " +
           "AND u.isActive = true")
    Page<User> searchActiveUsers(@Param("keyword") String keyword, Pageable pageable);
}