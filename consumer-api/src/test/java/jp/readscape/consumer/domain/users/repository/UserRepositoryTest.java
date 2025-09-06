package jp.readscape.consumer.domain.users.repository;

import jp.readscape.consumer.domain.users.model.Gender;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.domain.users.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User adminUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        // 一般ユーザー作成
        testUser = User.builder()
            .email("test@example.com")
            .password("$2a$10$hashedPassword")
            .name("テストユーザー")
            .phoneNumber("090-1234-5678")
            .address("東京都渋谷区1-1-1")
            .birthDate(LocalDate.of(1990, 5, 15))
            .gender(Gender.MALE)
            .role(UserRole.CONSUMER)
            .active(true)
            .emailVerified(true)
            .lastLoginAt(LocalDateTime.now().minusDays(1))
            .createdAt(LocalDateTime.now().minusDays(30))
            .updatedAt(LocalDateTime.now().minusDays(1))
            .build();
        testUser = entityManager.persistAndFlush(testUser);

        // 管理者ユーザー作成
        adminUser = User.builder()
            .email("admin@example.com")
            .password("$2a$10$hashedAdminPassword")
            .name("管理者ユーザー")
            .role(UserRole.ADMIN)
            .active(true)
            .emailVerified(true)
            .createdAt(LocalDateTime.now().minusDays(365))
            .updatedAt(LocalDateTime.now().minusDays(1))
            .build();
        adminUser = entityManager.persistAndFlush(adminUser);

        // 非アクティブユーザー作成
        inactiveUser = User.builder()
            .email("inactive@example.com")
            .password("$2a$10$hashedInactivePassword")
            .name("非アクティブユーザー")
            .role(UserRole.CONSUMER)
            .active(false)
            .emailVerified(false)
            .createdAt(LocalDateTime.now().minusDays(60))
            .updatedAt(LocalDateTime.now().minusDays(30))
            .build();
        inactiveUser = entityManager.persistAndFlush(inactiveUser);
    }

    @Test
    @DisplayName("メールアドレスでユーザー検索 - 正常系")
    void findByEmailSuccess() {
        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertThat(found).isPresent();
        User user = found.get();
        assertThat(user.getId()).isEqualTo(testUser.getId());
        assertThat(user.getName()).isEqualTo("テストユーザー");
        assertThat(user.getRole()).isEqualTo(UserRole.CONSUMER);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("メールアドレスでユーザー検索 - 存在しない")
    void findByEmailNotFound() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("大文字小文字を無視したメール検索")
    void findByEmailCaseInsensitive() {
        // Act
        Optional<User> foundLower = userRepository.findByEmail("test@example.com");
        Optional<User> foundUpper = userRepository.findByEmail("TEST@EXAMPLE.COM");
        Optional<User> foundMixed = userRepository.findByEmail("TeSt@ExAmPlE.CoM");

        // Assert
        assertThat(foundLower).isPresent();
        assertThat(foundUpper).isPresent();
        assertThat(foundMixed).isPresent();
        
        assertThat(foundLower.get().getId()).isEqualTo(testUser.getId());
        assertThat(foundUpper.get().getId()).isEqualTo(testUser.getId());
        assertThat(foundMixed.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("アクティブユーザーのみ検索")
    void findActiveUsers() {
        // Act
        List<User> activeUsers = userRepository.findByActiveTrue();

        // Assert
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting(User::getEmail)
            .containsExactlyInAnyOrder("test@example.com", "admin@example.com");
        assertThat(activeUsers).allMatch(User::isActive);
    }

    @Test
    @DisplayName("非アクティブユーザーのみ検索")
    void findInactiveUsers() {
        // Act
        List<User> inactiveUsers = userRepository.findByActiveFalse();

        // Assert
        assertThat(inactiveUsers).hasSize(1);
        assertThat(inactiveUsers.get(0).getEmail()).isEqualTo("inactive@example.com");
        assertThat(inactiveUsers.get(0).isActive()).isFalse();
    }

    @Test
    @DisplayName("ロール別ユーザー検索")
    void findByRole() {
        // Act
        List<User> consumers = userRepository.findByRole(UserRole.CONSUMER);
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        // Assert
        assertThat(consumers).hasSize(2);
        assertThat(consumers).extracting(User::getEmail)
            .containsExactlyInAnyOrder("test@example.com", "inactive@example.com");

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("メール認証済みユーザー検索")
    void findByEmailVerified() {
        // Act
        List<User> verifiedUsers = userRepository.findByEmailVerifiedTrue();
        List<User> unverifiedUsers = userRepository.findByEmailVerifiedFalse();

        // Assert
        assertThat(verifiedUsers).hasSize(2);
        assertThat(verifiedUsers).extracting(User::getEmail)
            .containsExactlyInAnyOrder("test@example.com", "admin@example.com");

        assertThat(unverifiedUsers).hasSize(1);
        assertThat(unverifiedUsers.get(0).getEmail()).isEqualTo("inactive@example.com");
    }

    @Test
    @DisplayName("名前での部分検索")
    void findByNameContaining() {
        // Act
        List<User> usersWithTest = userRepository.findByNameContainingIgnoreCase("テスト");
        List<User> usersWithUser = userRepository.findByNameContainingIgnoreCase("ユーザー");

        // Assert
        assertThat(usersWithTest).hasSize(1);
        assertThat(usersWithTest.get(0).getName()).isEqualTo("テストユーザー");

        assertThat(usersWithUser).hasSize(3);
    }

    @Test
    @DisplayName("作成日時範囲での検索")
    void findByCreatedAtBetween() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(90);
        LocalDateTime endDate = LocalDateTime.now().minusDays(20);

        // Act
        List<User> usersInRange = userRepository.findByCreatedAtBetween(startDate, endDate);

        // Assert
        assertThat(usersInRange).hasSize(2);
        assertThat(usersInRange).extracting(User::getEmail)
            .containsExactlyInAnyOrder("test@example.com", "inactive@example.com");
    }

    @Test
    @DisplayName("最終ログイン日時での検索")
    void findByLastLoginAt() {
        // Arrange
        LocalDateTime recentDate = LocalDateTime.now().minusDays(7);

        // Act
        List<User> recentUsers = userRepository.findByLastLoginAtAfter(recentDate);

        // Assert
        assertThat(recentUsers).hasSize(1);
        assertThat(recentUsers.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("複合条件での検索 - アクティブかつ認証済み")
    void findByActiveAndEmailVerified() {
        // Act
        List<User> activeVerifiedUsers = userRepository.findByActiveTrueAndEmailVerifiedTrue();

        // Assert
        assertThat(activeVerifiedUsers).hasSize(2);
        assertThat(activeVerifiedUsers).extracting(User::getEmail)
            .containsExactlyInAnyOrder("test@example.com", "admin@example.com");
    }

    @Test
    @DisplayName("年齢範囲での検索")
    void findByAgeRange() {
        // Arrange
        LocalDate startBirth = LocalDate.of(1985, 1, 1);
        LocalDate endBirth = LocalDate.of(1995, 12, 31);

        // Act
        List<User> usersInAgeRange = userRepository.findByBirthDateBetween(startBirth, endBirth);

        // Assert
        assertThat(usersInAgeRange).hasSize(1);
        assertThat(usersInAgeRange.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("性別での検索")
    void findByGender() {
        // 追加の女性ユーザーを作成
        User femaleUser = User.builder()
            .email("female@example.com")
            .password("password")
            .name("女性ユーザー")
            .gender(Gender.FEMALE)
            .role(UserRole.CONSUMER)
            .active(true)
            .build();
        entityManager.persistAndFlush(femaleUser);

        // Act
        List<User> maleUsers = userRepository.findByGender(Gender.MALE);
        List<User> femaleUsers = userRepository.findByGender(Gender.FEMALE);

        // Assert
        assertThat(maleUsers).hasSize(1);
        assertThat(maleUsers.get(0).getEmail()).isEqualTo("test@example.com");

        assertThat(femaleUsers).hasSize(1);
        assertThat(femaleUsers.get(0).getEmail()).isEqualTo("female@example.com");
    }

    @Test
    @DisplayName("ページネーションでのユーザー取得")
    void findAllWithPagination() {
        // Arrange: 追加ユーザーを作成
        for (int i = 0; i < 5; i++) {
            User user = User.builder()
                .email("user" + i + "@example.com")
                .password("password")
                .name("ユーザー" + i)
                .role(UserRole.CONSUMER)
                .active(true)
                .build();
            entityManager.persistAndFlush(user);
        }

        Pageable pageable = PageRequest.of(0, 3);

        // Act
        Page<User> userPage = userRepository.findAll(pageable);

        // Assert
        assertThat(userPage.getContent()).hasSize(3);
        assertThat(userPage.getTotalElements()).isEqualTo(8); // 3 existing + 5 new
        assertThat(userPage.getTotalPages()).isEqualTo(3);
        assertThat(userPage.hasNext()).isTrue();
    }

    @Test
    @DisplayName("ユーザーの統計情報取得")
    void getUserStatistics() {
        // Act
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countByActiveTrue();
        Long inactiveUsers = userRepository.countByActiveFalse();
        Long verifiedUsers = userRepository.countByEmailVerifiedTrue();
        Long consumers = userRepository.countByRole(UserRole.CONSUMER);
        Long admins = userRepository.countByRole(UserRole.ADMIN);

        // Assert
        assertThat(totalUsers).isEqualTo(3);
        assertThat(activeUsers).isEqualTo(2);
        assertThat(inactiveUsers).isEqualTo(1);
        assertThat(verifiedUsers).isEqualTo(2);
        assertThat(consumers).isEqualTo(2);
        assertThat(admins).isEqualTo(1);
    }

    @Test
    @DisplayName("メールアドレス重複チェック")
    void emailUniqueConstraint() {
        // Arrange
        User duplicateUser = User.builder()
            .email("test@example.com") // 既存のメールアドレス
            .password("password")
            .name("重複ユーザー")
            .role(UserRole.CONSUMER)
            .active(true)
            .build();

        // Act & Assert
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicateUser);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("メールアドレス存在チェック")
    void existsByEmail() {
        // Act
        boolean existsTestEmail = userRepository.existsByEmail("test@example.com");
        boolean existsNonExistentEmail = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(existsTestEmail).isTrue();
        assertThat(existsNonExistentEmail).isFalse();
    }

    @Test
    @DisplayName("ユーザー更新 - 最終ログイン時刻")
    void updateLastLoginAt() {
        // Arrange
        LocalDateTime newLoginTime = LocalDateTime.now();

        // Act
        testUser.setLastLoginAt(newLoginTime);
        User updatedUser = userRepository.save(testUser);
        entityManager.flush();

        // Assert
        User foundUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(foundUser.getLastLoginAt()).isEqualToIgnoringNanos(newLoginTime);
        assertThat(foundUser.getUpdatedAt()).isAfter(testUser.getCreatedAt());
    }

    @Test
    @DisplayName("ユーザー削除 - ソフトデリート")
    void softDeleteUser() {
        // Act
        testUser.setActive(false);
        userRepository.save(testUser);

        // Assert
        Optional<User> foundUser = userRepository.findById(testUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().isActive()).isFalse();

        // アクティブユーザー検索では除外される
        List<User> activeUsers = userRepository.findByActiveTrue();
        assertThat(activeUsers).doesNotContain(testUser);
    }

    @Test
    @DisplayName("パスワード変更")
    void changePassword() {
        // Arrange
        String newPassword = "$2a$10$newHashedPassword";

        // Act
        testUser.setPassword(newPassword);
        User updatedUser = userRepository.save(testUser);

        // Assert
        assertThat(updatedUser.getPassword()).isEqualTo(newPassword);
        assertThat(updatedUser.getUpdatedAt()).isAfter(testUser.getCreatedAt());
    }

    @Test
    @DisplayName("プロフィール情報更新")
    void updateProfile() {
        // Arrange
        String newName = "更新されたユーザー";
        String newPhoneNumber = "090-9876-5432";
        String newAddress = "大阪府大阪市2-2-2";

        // Act
        testUser.setName(newName);
        testUser.setPhoneNumber(newPhoneNumber);
        testUser.setAddress(newAddress);
        User updatedUser = userRepository.save(testUser);

        // Assert
        assertThat(updatedUser.getName()).isEqualTo(newName);
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(newPhoneNumber);
        assertThat(updatedUser.getAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("メール認証ステータス変更")
    void verifyEmail() {
        // Arrange - 未認証ユーザーのメール認証
        assertThat(inactiveUser.isEmailVerified()).isFalse();

        // Act
        inactiveUser.setEmailVerified(true);
        User updatedUser = userRepository.save(inactiveUser);

        // Assert
        assertThat(updatedUser.isEmailVerified()).isTrue();
        
        // 認証済みユーザー検索で含まれることを確認
        List<User> verifiedUsers = userRepository.findByEmailVerifiedTrue();
        assertThat(verifiedUsers).contains(updatedUser);
    }

    @Test
    @DisplayName("カスタムクエリ - 最近登録されたユーザー")
    void findRecentlyRegisteredUsers() {
        // Arrange
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(45);

        // Act
        List<User> recentUsers = userRepository.findByCreatedAtAfter(cutoffDate);

        // Assert
        assertThat(recentUsers).hasSize(2);
        assertThat(recentUsers).extracting(User::getEmail)
            .containsExactlyInAnyOrder("test@example.com", "inactive@example.com");
    }
}