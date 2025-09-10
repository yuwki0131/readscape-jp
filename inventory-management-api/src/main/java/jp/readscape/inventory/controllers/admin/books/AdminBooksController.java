package jp.readscape.inventory.controllers.admin.books;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jp.readscape.inventory.domain.users.model.User;
import jp.readscape.inventory.dto.admin.*;
import jp.readscape.inventory.exceptions.BookNotFoundException;
import jp.readscape.inventory.exceptions.DuplicateIsbnException;
import jp.readscape.inventory.services.AdminBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin Books", description = "管理者向け書籍管理API")
@SecurityRequirement(name = "bearerAuth")
public class AdminBooksController {

    private final AdminBookService adminBookService;

    @Operation(
        summary = "書籍一覧取得",
        description = "管理者向け書籍一覧を取得します。ページング、ステータスフィルタリングが可能です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍一覧取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping
    public ResponseEntity<Page<AdminBookView>> getBooks(
            @Parameter(description = "ページ番号（0から開始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "ページサイズ", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "書籍ステータス", example = "ACTIVE")
            @RequestParam(required = false) String status
    ) {
        log.info("GET /api/admin/books - page: {}, size: {}, status: {}", page, size, status);

        Page<AdminBookView> books = adminBookService.getBooks(page, size, status);
        return ResponseEntity.ok(books);
    }

    @Operation(
        summary = "書籍作成",
        description = "新規書籍を登録します。ISBN重複チェックが行われます。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "書籍作成成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です"),
        @ApiResponse(responseCode = "409", description = "ISBN重複エラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<?> createBook(
            @Valid @RequestBody CreateBookRequest request,
            Authentication auth
    ) {
        log.info("POST /api/admin/books - title: {}", request.getTitle());

        try {
            User user = (User) auth.getPrincipal();
            CreateBookResponse response = adminBookService.createBook(request, user.getId());
            
            log.info("Book created successfully: {} (ID: {})", response.getTitle(), response.getBookId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (DuplicateIsbnException e) {
            log.warn("ISBN duplication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(jp.readscape.inventory.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "書籍詳細取得",
        description = "指定された書籍IDの詳細情報を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍詳細取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookDetail(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long id
    ) {
        log.info("GET /api/admin/books/{}", id);

        try {
            AdminBookView bookView = adminBookService.getBookDetail(id);
            return ResponseEntity.ok(bookView);
        } catch (BookNotFoundException e) {
            log.warn("Book not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "書籍更新",
        description = "指定された書籍の情報を更新します。部分更新をサポートします。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍更新成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません"),
        @ApiResponse(responseCode = "409", description = "ISBN重複エラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long id,
            
            @Valid @RequestBody UpdateBookRequest request,
            Authentication auth
    ) {
        log.info("PUT /api/admin/books/{}", id);

        try {
            User user = (User) auth.getPrincipal();
            adminBookService.updateBook(id, request, user.getId());
            
            log.info("Book updated successfully: {}", id);
            return ResponseEntity.ok(jp.readscape.inventory.dto.ApiResponse.success("書籍情報を更新しました"));
            
        } catch (BookNotFoundException e) {
            log.warn("Book not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (DuplicateIsbnException e) {
            log.warn("ISBN duplication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(jp.readscape.inventory.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "書籍削除",
        description = "指定された書籍を削除します。ADMIN権限が必要です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍削除成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "ADMIN権限が必要です"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBook(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long id,
            Authentication auth
    ) {
        log.info("DELETE /api/admin/books/{}", id);

        try {
            User user = (User) auth.getPrincipal();
            adminBookService.deleteBook(id, user.getId());
            
            log.info("Book deleted successfully: {}", id);
            return ResponseEntity.ok(jp.readscape.inventory.dto.ApiResponse.success("書籍を削除しました"));
            
        } catch (BookNotFoundException e) {
            log.warn("Book not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "低在庫書籍一覧取得",
        description = "在庫が閾値以下の書籍一覧を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "低在庫書籍一覧取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/low-stock")
    public ResponseEntity<List<AdminBookView>> getLowStockBooks() {
        log.info("GET /api/admin/books/low-stock");

        List<AdminBookView> lowStockBooks = adminBookService.getLowStockBooks();
        return ResponseEntity.ok(lowStockBooks);
    }

    @Operation(
        summary = "在庫切れ書籍一覧取得",
        description = "在庫が0の書籍一覧を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫切れ書籍一覧取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<AdminBookView>> getOutOfStockBooks() {
        log.info("GET /api/admin/books/out-of-stock");

        List<AdminBookView> outOfStockBooks = adminBookService.getOutOfStockBooks();
        return ResponseEntity.ok(outOfStockBooks);
    }

    @Operation(
        summary = "カテゴリ一覧取得",
        description = "書籍に設定されているカテゴリの一覧を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "カテゴリ一覧取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        log.info("GET /api/admin/books/categories");

        List<String> categories = adminBookService.getCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
        summary = "書籍検索",
        description = "キーワードで書籍を検索します。タイトル、著者、カテゴリが検索対象です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "書籍検索成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<AdminBookView>> searchBooks(
            @Parameter(description = "検索キーワード", example = "Spring", required = true)
            @RequestParam String keyword,
            
            @Parameter(description = "ページ番号（0から開始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "ページサイズ", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/admin/books/search - keyword: {}", keyword);

        Page<AdminBookView> books = adminBookService.searchBooks(keyword, page, size);
        return ResponseEntity.ok(books);
    }
}