package jp.readscape.consumer.constants;

/**
 * 注文関連の定数クラス
 */
public final class OrderConstants {

    private OrderConstants() {
        // 定数クラスのインスタンス化を防ぐ
    }

    /**
     * 注文番号のプレフィックス
     */
    public static final String ORDER_NUMBER_PREFIX = "ORD-";

    /**
     * 注文のキャンセル可能期間（日数）
     */
    public static final int CANCELLATION_PERIOD_DAYS = 5;

    /**
     * デフォルトのページサイズ
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大ページサイズ
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * ポピュラー書籍の最大取得件数
     */
    public static final int MAX_POPULAR_BOOKS_LIMIT = 50;
}