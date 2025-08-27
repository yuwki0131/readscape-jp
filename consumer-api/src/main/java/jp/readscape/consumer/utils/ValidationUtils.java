package jp.readscape.consumer.utils;

import jp.readscape.consumer.constants.OrderConstants;

/**
 * API コントローラー共通のバリデーションユーティリティクラス
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // ユーティリティクラスのインスタンス化を防ぐ
    }

    /**
     * ページング パラメータの バリデーション
     * 
     * @param page ページ番号（0から開始）
     * @param size ページサイズ
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    public static void validatePagingParameters(Integer page, Integer size) {
        if (page == null || page < 0) {
            throw new IllegalArgumentException("ページ番号は0以上である必要があります");
        }
        if (size == null || size <= 0 || size > OrderConstants.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("ページサイズは1以上" + OrderConstants.MAX_PAGE_SIZE + "以下である必要があります");
        }
    }

    /**
     * ID パラメータのバリデーション（正の整数チェック）
     * 
     * @param id ID値
     * @param fieldName フィールド名（エラーメッセージ用）
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    public static void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "は正の整数である必要があります");
        }
    }

    /**
     * limit パラメータのバリデーション
     * 
     * @param limit 取得件数制限
     * @param maxLimit 最大取得件数
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    public static void validateLimit(Integer limit, int maxLimit) {
        if (limit == null || limit <= 0 || limit > maxLimit) {
            throw new IllegalArgumentException("取得件数は1以上" + maxLimit + "以下である必要があります");
        }
    }

    /**
     * 必須文字列パラメータのバリデーション
     * 
     * @param value 検証対象の文字列
     * @param fieldName フィールド名（エラーメッセージ用）
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    public static void validateRequiredString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "は必須です");
        }
    }
}