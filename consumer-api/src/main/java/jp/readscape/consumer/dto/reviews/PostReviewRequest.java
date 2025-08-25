package jp.readscape.consumer.dto.reviews;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostReviewRequest {

    @NotNull(message = "評価は必須です")
    @Min(value = 1, message = "評価は1以上である必要があります")
    @Max(value = 5, message = "評価は5以下である必要があります")
    private Integer rating;

    @Size(max = 100, message = "レビュータイトルは100文字以下で入力してください")
    private String title;

    @Size(max = 2000, message = "レビューコメントは2000文字以下で入力してください")
    private String comment;
}