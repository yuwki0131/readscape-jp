package jp.readscape.inventory.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "書籍作成レスポンス")
public class CreateBookResponse {

    @Schema(description = "作成された書籍ID", example = "1")
    private Long bookId;

    @Schema(description = "タイトル", example = "Spring Boot実践ガイド")
    private String title;

    @Schema(description = "ISBN", example = "9784798142470")
    private String isbn;

    @Schema(description = "作成日時", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "成功メッセージ", example = "書籍が正常に作成されました")
    private String message;

    // fromメソッドはDtoMappingServiceに移動されました
}