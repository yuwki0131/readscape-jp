package jp.readscape.consumer.dto.books;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "書籍一覧レスポンス")
public class BooksResponse {

    @Schema(description = "書籍一覧")
    private List<BookSummary> books;

    @Schema(description = "現在のページ番号", example = "0")
    @JsonProperty("current_page")
    private Integer currentPage;

    @Schema(description = "総ページ数", example = "5")
    @JsonProperty("total_pages")
    private Integer totalPages;

    @Schema(description = "総要素数", example = "50")
    @JsonProperty("total_elements")
    private Long totalElements;

    @Schema(description = "ページサイズ", example = "10")
    private Integer size;

    @Schema(description = "次ページの有無", example = "true")
    @JsonProperty("has_next")
    private Boolean hasNext;

    @Schema(description = "前ページの有無", example = "false")
    @JsonProperty("has_previous")
    private Boolean hasPrevious;

    // ビジネスロジック用メソッド

    @Schema(description = "書籍数", example = "10")
    @JsonProperty("count")
    public Integer getCount() {
        return books != null ? books.size() : 0;
    }

    @Schema(description = "空のリストかどうか", example = "false")
    @JsonProperty("is_empty")
    public Boolean getIsEmpty() {
        return books == null || books.isEmpty();
    }

    @Schema(description = "最初のページかどうか", example = "true")
    @JsonProperty("is_first")
    public Boolean getIsFirst() {
        return currentPage != null && currentPage == 0;
    }

    @Schema(description = "最後のページかどうか", example = "false")
    @JsonProperty("is_last")
    public Boolean getIsLast() {
        return hasNext != null && !hasNext;
    }

    @Schema(description = "ページネーション情報", example = "Page 1 of 5 (50 total)")
    @JsonProperty("pagination_info")
    public String getPaginationInfo() {
        if (totalPages == null || totalElements == null) {
            return "";
        }
        
        int displayPage = (currentPage != null ? currentPage : 0) + 1;
        return String.format("Page %d of %d (%d total)", displayPage, totalPages, totalElements);
    }
}