package jp.readscape.consumer.domain.users.model;

public enum UserRole {
    CONSUMER("ROLE_CONSUMER", "一般消費者"),
    ADMIN("ROLE_ADMIN", "システム管理者"),
    MANAGER("ROLE_MANAGER", "在庫管理者"),
    ANALYST("ROLE_ANALYST", "分析担当者");
    
    private final String authority;
    private final String description;
    
    UserRole(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }
    
    public String getAuthority() {
        return authority;
    }
    
    public String getDescription() {
        return description;
    }
}