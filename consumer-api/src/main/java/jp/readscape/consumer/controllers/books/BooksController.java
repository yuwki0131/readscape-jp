package jp.readscape.consumer.controllers.books;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.readscape.consumer.constants.SortConstants;
import jp.readscape.consumer.dto.books.BookDetail;
import jp.readscape.consumer.dto.books.BookSummary;
import jp.readscape.consumer.dto.books.BooksResponse;
import jp.readscape.consumer.services.BookService;
import jp.readscape.consumer.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "書籍閲覧API")
public class BooksController {

    private final BookService bookService;

    @Operation(
        summary = "書籍一覧取得",
        description = "検索条件に基づいて書籍一覧を取得します。カテゴリー、キーワード、ソート条件を指定できます。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍一覧取得成功"),
        @ApiResponse(responseCode = "400", description = "パラメータエラー", 
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<BooksResponse> getBooks(
            @Parameter(description = "カテゴリー名（部分一致）", example = "技術書")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "検索キーワード（タイトル・著者名）", example = "Spring")
            @RequestParam(required = false) String keyword,
            
            @Parameter(description = "ページ番号（0から開始）", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "ページサイズ", example = "10")
            @RequestParam(defaultValue = "10") Integer size,
            
            @Parameter(description = "ソート条件", example = "newest",
                schema = @Schema(allowableValues = {"title", "author", "price_asc", "price_desc", "rating", "popularity", "newest", "oldest"}))
            @RequestParam(defaultValue = "newest") String sortBy
    ) {
        log.info("GET /books - category: {}, keyword: {}, page: {}, size: {}, sortBy: {}", 
                category, keyword, page, size, sortBy);

        // パラメータバリデーション
        ValidationUtils.validatePagingParameters(page, size);
        String validatedSortBy = SortConstants.validateAndGetSortBy(
            sortBy, SortConstants.BookSort.VALID_SORT_VALUES, SortConstants.BookSort.DEFAULT);

        BooksResponse response = bookService.findBooks(category, keyword, page, size, validatedSortBy);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "書籍詳細取得",
        description = "指定されたIDの書籍詳細情報を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍詳細取得成功"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookDetail> getBookById(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long id
    ) {
        log.info("GET /books/{} - bookId: {}", id, id);

        ValidationUtils.validatePositiveId(id, "書籍ID");

        BookDetail bookDetail = bookService.findBookById(id);
        return ResponseEntity.ok(bookDetail);
    }

    @Operation(
        summary = "書籍検索（キーワード）",
        description = "キーワードでタイトルまたは著者名を検索します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "検索成功"),
        @ApiResponse(responseCode = "400", description = "パラメータエラー")
    })
    @GetMapping("/search")
    public ResponseEntity<BooksResponse> searchBooks(
            @Parameter(description = "検索キーワード", example = "Java", required = true)
            @RequestParam String q,
            
            @Parameter(description = "カテゴリー絞り込み", example = "技術書")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "ページ番号", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "ページサイズ", example = "10")
            @RequestParam(defaultValue = "10") Integer size,
            
            @Parameter(description = "ソート条件", example = "relevance")
            @RequestParam(defaultValue = "relevance") String sortBy
    ) {
        log.info("GET /books/search - query: {}, category: {}, page: {}, size: {}", 
                q, category, page, size);

        ValidationUtils.validateRequiredString(q, "検索キーワード");
        ValidationUtils.validatePagingParameters(page, size);
        String validatedSortBy = SortConstants.validateAndGetSortBy(
            sortBy, SortConstants.BookSort.VALID_SORT_VALUES, SortConstants.BookSort.DEFAULT);

        BooksResponse response = bookService.findBooks(category, q.trim(), page, size, validatedSortBy);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "カテゴリー一覧取得",
        description = "利用可能な全カテゴリー一覧を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "カテゴリー一覧取得成功")
    })
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        log.info("GET /books/categories");

        List<String> categories = bookService.findAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
        summary = "人気書籍一覧取得",
        description = "レビュー数の多い人気書籍を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "人気書籍一覧取得成功")
    })
    @GetMapping("/popular")
    public ResponseEntity<List<BookSummary>> getPopularBooks(
            @Parameter(description = "取得件数", example = "10")
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("GET /books/popular - limit: {}", limit);

        ValidationUtils.validateLimit(limit, 50);

        List<BookSummary> popularBooks = bookService.findPopularBooks(limit);
        return ResponseEntity.ok(popularBooks);
    }

    @Operation(
        summary = "評価の高い書籍一覧取得",
        description = "平均評価の高い書籍を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "高評価書籍一覧取得成功")
    })
    @GetMapping("/top-rated")
    public ResponseEntity<List<BookSummary>> getTopRatedBooks(
            @Parameter(description = "取得件数", example = "10")
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("GET /books/top-rated - limit: {}", limit);

        ValidationUtils.validateLimit(limit, 50);

        List<BookSummary> topRatedBooks = bookService.findTopRatedBooks(limit);
        return ResponseEntity.ok(topRatedBooks);
    }

    @Operation(
        summary = "在庫のある書籍一覧取得",
        description = "在庫のある書籍のみを取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫書籍一覧取得成功")
    })
    @GetMapping("/in-stock")
    public ResponseEntity<BooksResponse> getBooksInStock(
            @Parameter(description = "ページ番号", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "ページサイズ", example = "10")
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("GET /books/in-stock - page: {}, size: {}", page, size);

        ValidationUtils.validatePagingParameters(page, size);

        BooksResponse response = bookService.findBooksInStock(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "ISBN検索",
        description = "ISBNによる書籍検索を行います"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍詳細取得成功"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません")
    })
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDetail> getBookByIsbn(
            @Parameter(description = "ISBN", example = "9784000000001", required = true)
            @PathVariable String isbn
    ) {
        log.info("GET /books/isbn/{} - isbn: {}", isbn, isbn);

        ValidationUtils.validateRequiredString(isbn, "ISBN");

        BookDetail bookDetail = bookService.findBookByIsbn(isbn.trim());
        return ResponseEntity.ok(bookDetail);
    }
}