package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "書籍作成リクエスト")
public class CreateBookRequest {

    @NotBlank(message = "タイトルは必須です")
    @Size(max = 255, message = "タイトルは255文字以内で入力してください")
    @Schema(description = "書籍タイトル", example = "Spring Boot実践ガイド")
    private String title;

    @NotBlank(message = "著者名は必須です")
    @Size(max = 255, message = "著者名は255文字以内で入力してください")
    @Schema(description = "著者名", example = "田中太郎")
    private String author;

    @Pattern(regexp = "^[0-9]{10}|[0-9]{13}$", message = "ISBNは10桁または13桁の数字で入力してください")
    @Schema(description = "ISBN", example = "9784798142470")
    private String isbn;

    @NotNull(message = "価格は必須です")
    @Min(value = 1, message = "価格は1円以上で入力してください")
    @Max(value = 999999, message = "価格は999,999円以下で入力してください")
    @Schema(description = "価格", example = "2980")
    private Integer price;

    @NotNull(message = "在庫数は必須です")
    @Min(value = 0, message = "在庫数は0以上で入力してください")
    @Schema(description = "初期在庫数", example = "50")
    private Integer stockQuantity;

    @Min(value = 1, message = "低在庫閾値は1以上で入力してください")
    @Schema(description = "低在庫閾値", example = "10")
    private Integer lowStockThreshold;

    @Size(max = 2000, message = "説明は2000文字以内で入力してください")
    @Schema(description = "書籍説明", example = "Spring Bootの基礎から応用まで実践的に学べる入門書です。")
    private String description;

    @Size(max = 500, message = "画像URLは500文字以内で入力してください")
    @Schema(description = "書籍画像URL", example = "https://example.com/images/spring-boot-guide.jpg")
    private String imageUrl;

    @Size(max = 100, message = "カテゴリは100文字以内で入力してください")
    @Schema(description = "カテゴリ", example = "プログラミング")
    private String category;

    @Size(max = 255, message = "出版社名は255文字以内で入力してください")
    @Schema(description = "出版社", example = "技術評論社")
    private String publisher;

    @Schema(description = "出版日", example = "2024-01-15T00:00:00")
    private LocalDateTime publishedDate;

    // バリデーション用メソッド
    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 10;
    }
}