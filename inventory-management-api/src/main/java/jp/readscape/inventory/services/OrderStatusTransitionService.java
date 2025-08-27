package jp.readscape.inventory.services;

import jp.readscape.inventory.domain.orders.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 注文ステータス遷移の管理を専門とするサービス
 * Open/Closed Principleに従い、拡張しやすい設計
 */
@Slf4j
@Service
public class OrderStatusTransitionService {

    // ステータス遷移マップ（状態機械パターン）
    private static final Map<Order.OrderStatus, Set<Order.OrderStatus>> ALLOWED_TRANSITIONS;
    
    // ステータス表示名マップ
    private static final Map<Order.OrderStatus, String> STATUS_DISPLAY_NAMES;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(Order.OrderStatus.class);
        ALLOWED_TRANSITIONS.put(Order.OrderStatus.PENDING, 
            Set.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(Order.OrderStatus.CONFIRMED, 
            Set.of(Order.OrderStatus.PROCESSING, Order.OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(Order.OrderStatus.PROCESSING, 
            Set.of(Order.OrderStatus.SHIPPED));
        ALLOWED_TRANSITIONS.put(Order.OrderStatus.SHIPPED, 
            Set.of(Order.OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(Order.OrderStatus.DELIVERED, Set.of());
        ALLOWED_TRANSITIONS.put(Order.OrderStatus.CANCELLED, Set.of());

        STATUS_DISPLAY_NAMES = new EnumMap<>(Order.OrderStatus.class);
        STATUS_DISPLAY_NAMES.put(Order.OrderStatus.PENDING, "注文受付");
        STATUS_DISPLAY_NAMES.put(Order.OrderStatus.CONFIRMED, "注文確定");
        STATUS_DISPLAY_NAMES.put(Order.OrderStatus.PROCESSING, "処理中");
        STATUS_DISPLAY_NAMES.put(Order.OrderStatus.SHIPPED, "配送中");
        STATUS_DISPLAY_NAMES.put(Order.OrderStatus.DELIVERED, "配送完了");
        STATUS_DISPLAY_NAMES.put(Order.OrderStatus.CANCELLED, "キャンセル");
    }

    /**
     * ステータス遷移が有効かどうかを判定
     * 
     * @param currentStatus 現在のステータス
     * @param newStatus 新しいステータス
     * @return 遷移が有効な場合true
     */
    public boolean isValidTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        Set<Order.OrderStatus> allowedTransitions = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(newStatus);
    }

    /**
     * ステータス遷移のバリデーション
     * 
     * @param currentStatus 現在のステータス
     * @param newStatus 新しいステータス
     * @throws IllegalArgumentException 不正な遷移の場合
     */
    public void validateTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        if (!isValidTransition(currentStatus, newStatus)) {
            String message = String.format("不正なステータス遷移: %s -> %s。%sから遷移可能なステータス: %s",
                getDisplayName(currentStatus),
                getDisplayName(newStatus),
                getDisplayName(currentStatus),
                getAllowedNextStatuses(currentStatus));
            
            log.warn("Invalid status transition attempted: {} -> {}", currentStatus, newStatus);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * ステータス表示名を取得
     * 
     * @param status ステータス
     * @return 表示名
     */
    public String getDisplayName(Order.OrderStatus status) {
        return STATUS_DISPLAY_NAMES.getOrDefault(status, status.toString());
    }

    /**
     * 指定されたステータスから遷移可能なステータス一覧を取得
     * 
     * @param currentStatus 現在のステータス
     * @return 遷移可能なステータスのセット
     */
    public Set<Order.OrderStatus> getAllowedNextStatuses(Order.OrderStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }

    /**
     * 注文がキャンセル可能かどうかを判定
     * 
     * @param currentStatus 現在のステータス
     * @return キャンセル可能な場合true
     */
    public boolean isCancellable(Order.OrderStatus currentStatus) {
        return isValidTransition(currentStatus, Order.OrderStatus.CANCELLED);
    }

    /**
     * 注文が処理済みかどうかを判定
     * 
     * @param currentStatus 現在のステータス
     * @return 処理済みの場合true
     */
    public boolean isProcessed(Order.OrderStatus currentStatus) {
        return currentStatus == Order.OrderStatus.SHIPPED || 
               currentStatus == Order.OrderStatus.DELIVERED;
    }

    /**
     * 注文がアクティブかどうかを判定
     * 
     * @param currentStatus 現在のステータス
     * @return アクティブな場合true
     */
    public boolean isActive(Order.OrderStatus currentStatus) {
        return currentStatus != Order.OrderStatus.CANCELLED;
    }
}