package jp.readscape.inventory.domain.users.model;

import lombok.Getter;

/**
 * ユーザーロール列挙型
 * システム内でのユーザーの権限レベルを定義
 */
@Getter
public enum UserRole {
    /**
     * 一般消費者
     * - 書籍の閲覧・購入
     * - レビューの投稿
     * - 注文履歴の確認
     */
    CONSUMER("ROLE_CONSUMER", "一般消費者", 1),
    
    /**
     * 分析担当者
     * - 売上・在庫データの分析
     * - レポートの閲覧
     */
    ANALYST("ROLE_ANALYST", "分析担当者", 2),
    
    /**
     * 在庫管理者
     * - 在庫管理
     * - 書籍情報の更新
     * - 注文処理
     */
    MANAGER("ROLE_MANAGER", "在庫管理者", 3),
    
    /**
     * システム管理者
     * - 全システム機能へのアクセス
     * - ユーザー管理
     * - システム設定
     */
    ADMIN("ROLE_ADMIN", "システム管理者", 4);

    private final String authority;
    private final String description;
    private final int level;

    UserRole(String authority, String description, int level) {
        this.authority = authority;
        this.description = description;
        this.level = level;
    }

    /**
     * 権限レベルが指定されたロール以上かどうかを判定
     * 
     * @param role 比較対象のロール
     * @return 指定されたロール以上の場合 true
     */
    public boolean hasLevelOrHigher(UserRole role) {
        return this.level >= role.level;
    }

    /**
     * Spring Securityの権限文字列から対応するロールを取得
     * 
     * @param authority 権限文字列
     * @return 対応するUserRole
     * @throws IllegalArgumentException 対応するロールが見つからない場合
     */
    public static UserRole fromAuthority(String authority) {
        for (UserRole role : values()) {
            if (role.authority.equals(authority)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown authority: " + authority);
    }

    /**
     * ロール名から対応するロールを取得
     * 
     * @param name ロール名
     * @return 対応するUserRole
     * @throws IllegalArgumentException 対応するロールが見つからない場合
     */
    public static UserRole fromName(String name) {
        try {
            return UserRole.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role name: " + name, e);
        }
    }

    @Override
    public String toString() {
        return authority;
    }
}