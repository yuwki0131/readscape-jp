package jp.readscape.consumer.constants;

import java.util.Set;

/**
 * ソート条件定数クラス
 */
public final class SortConstants {

    private SortConstants() {
        // 定数クラスのインスタンス化を防ぐ
    }

    /**
     * 書籍一覧のソート条件
     */
    public static final class BookSort {
        public static final String TITLE = "title";
        public static final String AUTHOR = "author";
        public static final String PRICE_ASC = "price_asc";
        public static final String PRICE_DESC = "price_desc";
        public static final String RATING = "rating";
        public static final String POPULARITY = "popularity";
        public static final String NEWEST = "newest";
        public static final String OLDEST = "oldest";
        public static final String RELEVANCE = "relevance";
        public static final String DEFAULT = NEWEST;

        /**
         * 有効なソート条件のセット
         */
        public static final Set<String> VALID_SORT_VALUES = Set.of(
            TITLE, AUTHOR, PRICE_ASC, PRICE_DESC, RATING, 
            POPULARITY, NEWEST, OLDEST, RELEVANCE
        );

        private BookSort() {}
    }

    /**
     * レビュー一覧のソート条件
     */
    public static final class ReviewSort {
        public static final String HELPFUL = "helpful";
        public static final String POSITIVE = "positive";
        public static final String NEGATIVE = "negative";
        public static final String VERIFIED = "verified";
        public static final String NEWEST = "newest";
        public static final String DEFAULT = NEWEST;

        /**
         * 有効なソート条件のセット
         */
        public static final Set<String> VALID_SORT_VALUES = Set.of(
            HELPFUL, POSITIVE, NEGATIVE, VERIFIED, NEWEST
        );

        private ReviewSort() {}
    }

    /**
     * ソート条件バリデーション
     * 
     * @param sortBy ソート条件
     * @param validValues 有効な値のセット
     * @param defaultValue デフォルト値
     * @return バリデーション済みのソート条件
     */
    public static String validateAndGetSortBy(String sortBy, Set<String> validValues, String defaultValue) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return defaultValue;
        }

        String trimmedSortBy = sortBy.trim().toLowerCase();
        if (!validValues.contains(trimmedSortBy)) {
            // 無効なソート条件の場合はデフォルト値を返す（例外は投げない）
            return defaultValue;
        }

        return trimmedSortBy;
    }
}