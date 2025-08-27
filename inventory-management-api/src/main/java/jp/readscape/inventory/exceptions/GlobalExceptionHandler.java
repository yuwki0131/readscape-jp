package jp.readscape.inventory.exceptions;

import jp.readscape.inventory.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * グローバル例外ハンドラー
 * アプリケーション全体の例外処理を統一管理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * バリデーションエラーの処理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error occurred: {}", ex.getMessage());
        
        BindingResult result = ex.getBindingResult();
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("error", "VALIDATION_ERROR");
        errorResponse.put("message", "入力値に誤りがあります");
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("timestamp", LocalDateTime.now());
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : result.getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        errorResponse.put("fieldErrors", fieldErrors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 書籍が見つからない場合の例外処理
     */
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiResponse> handleBookNotFoundException(BookNotFoundException ex) {
        log.warn("Book not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 注文が見つからない場合の例外処理
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse> handleOrderNotFoundException(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * ISBN重複の例外処理
     */
    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ApiResponse> handleDuplicateIsbnException(DuplicateIsbnException ex) {
        log.warn("Duplicate ISBN error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 在庫不足の例外処理
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse> handleInsufficientStockException(InsufficientStockException ex) {
        log.warn("Insufficient stock error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * アクセス拒否の例外処理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("アクセス権限がありません"));
    }

    /**
     * 不正な引数の例外処理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 不正な状態の例外処理
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * ランタイム例外の処理
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("内部サーバーエラーが発生しました"));
    }

    /**
     * その他すべての例外の処理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("予期しないエラーが発生しました"));
    }
}