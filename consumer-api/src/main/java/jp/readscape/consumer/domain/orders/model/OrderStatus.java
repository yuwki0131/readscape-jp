package jp.readscape.consumer.domain.orders.model;

public enum OrderStatus {
    PENDING("注文確認中"),
    CONFIRMED("注文確定"),
    PROCESSING("準備中"),
    SHIPPED("発送済み"),
    DELIVERED("配送完了"),
    CANCELLED("キャンセル"),
    RETURNED("返品");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}