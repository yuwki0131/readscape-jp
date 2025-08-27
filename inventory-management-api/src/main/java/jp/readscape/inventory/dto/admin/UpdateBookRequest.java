package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import jp.readscape.inventory.domain.books.model.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "書籍更新リクエスト")
public class UpdateBookRequest {

    @Size(max = 255, message = "タイトルは255文字以内で入力してください")
    @Schema(description = "書籍タイトル", example = "Spring Boot実践ガイド 改訂版")
    private String title;

    @Size(max = 255, message = "著者名は255文字以内で入力してください")
    @Schema(description = "著者名", example = "田中太郎")
    private String author;

    @Pattern(regexp = "^[0-9]{10}|[0-9]{13}$", message = "ISBNは10桁または13桁の数字で入力してください")
    @Schema(description = "ISBN", example = "9784798142470")
    private String isbn;

    @Min(value = 1, message = "価格は1円以上で入力してください")
    @Max(value = 999999, message = "価格は999,999円以下で入力してください")
    @Schema(description = "価格", example = "3280")
    private Integer price;

    @Min(value = 1, message = "低在庫閾値は1以上で入力してください")
    @Schema(description = "低在庫閾値", example = "15")
    private Integer lowStockThreshold;

    @Size(max = 2000, message = "説明は2000文字以内で入力してください")
    @Schema(description = "書籍説明")
    private String description;

    @Size(max = 500, message = "画像URLは500文字以内で入力してください")
    @Schema(description = "書籍画像URL")
    private String imageUrl;

    @Size(max = 100, message = "カテゴリは100文字以内で入力してください")
    @Schema(description = "カテゴリ", example = "プログラミング")
    private String category;

    @Size(max = 255, message = "出版社名は255文字以内で入力してください")
    @Schema(description = "出版社", example = "技術評論社")
    private String publisher;

    @Schema(description = "出版日", example = "2024-01-15T00:00:00")
    private LocalDateTime publishedDate;

    @Schema(description = "書籍ステータス", example = "ACTIVE")
    private Book.BookStatus status;

    // Null チェック用ヘルパーメソッド
    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }

    public boolean hasAuthor() {
        return author != null && !author.trim().isEmpty();
    }

    public boolean hasIsbn() {
        return isbn != null && !isbn.trim().isEmpty();
    }

    public boolean hasPrice() {
        return price != null;
    }

    public boolean hasDescription() {
        return description != null;
    }

    public boolean hasImageUrl() {
        return imageUrl != null;
    }

    public boolean hasCategory() {
        return category != null;
    }

    public boolean hasPublisher() {
        return publisher != null;
    }

    public boolean hasPublishedDate() {
        return publishedDate != null;
    }

    public boolean hasStatus() {
        return status != null;
    }

    public boolean hasLowStockThreshold() {
        return lowStockThreshold != null;
    }
}