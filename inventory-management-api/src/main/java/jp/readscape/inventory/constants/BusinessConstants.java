package jp.readscape.inventory.constants;

/**
 * ビジネスロジックで使用される定数クラス
 */
public final class BusinessConstants {
    
    // 在庫関連定数
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    public static final int MINIMUM_STOCK_QUANTITY = 0;
    public static final int RECOMMENDED_ORDER_MULTIPLIER = 2;
    
    // 時間関連定数
    public static final int URGENT_ORDER_THRESHOLD_DAYS = 3;
    public static final int DELAYED_ORDER_THRESHOLD_DAYS = 3;
    public static final int HIGH_PRIORITY_THRESHOLD_HOURS = 72;
    public static final int MEDIUM_PRIORITY_THRESHOLD_HOURS = 48;
    public static final int LOW_PRIORITY_THRESHOLD_HOURS = 24;
    
    // ページング関連定数
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;
    
    // 価格関連定数
    public static final int MIN_BOOK_PRICE = 1;
    public static final int MAX_BOOK_PRICE = 999999;
    
    // バリデーション関連定数
    public static final int MAX_TITLE_LENGTH = 255;
    public static final int MAX_AUTHOR_LENGTH = 255;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;
    public static final int MAX_REASON_LENGTH = 500;
    public static final int MAX_REFERENCE_NUMBER_LENGTH = 100;
    
    // 統計計算関連定数
    public static final int ALERT_LEVEL_DIVISOR = 2;
    public static final int EXPECTED_STATS_COUNT = 3;
    public static final int AVERAGE_STOCK_INDEX = 2;
    
    // プライベートコンストラクタでインスタンス化を防ぐ
    private BusinessConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }
}