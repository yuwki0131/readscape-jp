package jp.readscape.inventory.controllers.admin.inventory;

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
import jp.readscape.inventory.dto.inventory.*;
import jp.readscape.inventory.exceptions.BookNotFoundException;
import jp.readscape.inventory.exceptions.InsufficientStockException;
import jp.readscape.inventory.services.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin Inventory", description = "管理者向け在庫管理API")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(
        summary = "在庫一覧取得",
        description = "全書籍の在庫情報を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫一覧取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping
    public ResponseEntity<List<InventoryItem>> getInventory() {
        log.info("GET /api/admin/inventory");

        List<InventoryItem> inventory = inventoryService.getInventory();
        return ResponseEntity.ok(inventory);
    }

    @Operation(
        summary = "在庫更新",
        description = "指定された書籍の在庫を更新します。入荷、出荷、調整などの操作が可能です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫更新成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません"),
        @ApiResponse(responseCode = "409", description = "在庫不足エラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class)))
    })
    @PostMapping("/{bookId}/stock")
    public ResponseEntity<?> updateStock(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId,
            
            @Valid @RequestBody StockUpdateRequest request,
            Authentication auth
    ) {
        log.info("POST /api/admin/inventory/{}/stock - type: {}, quantity: {}", 
                 bookId, request.getType(), request.getQuantity());

        try {
            User user = (User) auth.getPrincipal();
            inventoryService.updateStock(bookId, request, user.getId());
            
            log.info("Stock updated successfully for book {} by user {}", bookId, user.getUsername());
            return ResponseEntity.ok(jp.readscape.inventory.dto.ApiResponse.success("在庫を更新しました"));
            
        } catch (BookNotFoundException e) {
            log.warn("Book not found: {}", bookId);
            return ResponseEntity.notFound().build();
        } catch (InsufficientStockException e) {
            log.warn("Insufficient stock for book {}: {}", bookId, e.getMessage());
            return ResponseEntity.status(409)
                    .body(jp.readscape.inventory.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "書籍別在庫詳細取得",
        description = "指定された書籍の詳細な在庫情報を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫詳細取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません")
    })
    @GetMapping("/{bookId}")
    public ResponseEntity<?> getBookInventory(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId
    ) {
        log.info("GET /api/admin/inventory/{}", bookId);

        try {
            InventoryItem inventory = inventoryService.getBookInventory(bookId);
            return ResponseEntity.ok(inventory);
        } catch (BookNotFoundException e) {
            log.warn("Book not found: {}", bookId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "低在庫商品一覧取得",
        description = "在庫が閾値以下の商品一覧を取得します。緊急度も含まれます。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "低在庫商品一覧取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockItem>> getLowStockItems() {
        log.info("GET /api/admin/inventory/low-stock");

        List<LowStockItem> lowStockItems = inventoryService.getLowStockItems();
        return ResponseEntity.ok(lowStockItems);
    }

    @Operation(
        summary = "在庫変動履歴取得",
        description = "在庫変動の履歴を取得します。書籍や期間での絞り込みが可能です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫履歴取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<StockHistoryResponse>> getStockHistory(
            @Parameter(description = "書籍ID", example = "1")
            @RequestParam(required = false) Long bookId,
            
            @Parameter(description = "開始日", example = "2024-01-01")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "終了日", example = "2024-01-31")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "ページ番号（0から開始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "ページサイズ", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/admin/inventory/history - bookId: {}, startDate: {}, endDate: {}", 
                 bookId, startDate, endDate);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        Page<StockHistoryResponse> history = inventoryService.getStockHistory(
                bookId, startDateTime, endDateTime, page, size);
        return ResponseEntity.ok(history);
    }

    @Operation(
        summary = "在庫統計取得",
        description = "在庫の全体統計情報を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫統計取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/statistics")
    public ResponseEntity<InventoryService.InventoryStatistics> getInventoryStatistics() {
        log.info("GET /api/admin/inventory/statistics");

        InventoryService.InventoryStatistics statistics = inventoryService.getInventoryStatistics();
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "期間内在庫変動統計取得",
        description = "指定期間内の在庫変動統計を取得します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "在庫変動統計取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @GetMapping("/movement-statistics")
    public ResponseEntity<InventoryService.StockMovementStatistics> getStockMovementStatistics(
            @Parameter(description = "開始日", example = "2024-01-01", required = true)
            @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "終了日", example = "2024-01-31", required = true)
            @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /api/admin/inventory/movement-statistics - startDate: {}, endDate: {}", 
                 startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        InventoryService.StockMovementStatistics statistics = 
                inventoryService.getStockMovementStatistics(startDateTime, endDateTime);
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "一括在庫調整",
        description = "複数の書籍の在庫を一括で調整します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "一括在庫調整成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @PostMapping("/bulk-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkStockUpdate(
            @Valid @RequestBody List<InventoryService.BulkStockUpdateRequest> requests,
            Authentication auth
    ) {
        log.info("POST /api/admin/inventory/bulk-update - {} items", requests.size());

        User user = (User) auth.getPrincipal();
        inventoryService.bulkStockAdjustment(requests, user.getId());
        
        log.info("Bulk stock adjustment completed by user: {}", user.getUsername());
        return ResponseEntity.ok(jp.readscape.inventory.dto.ApiResponse.success("一括在庫調整を完了しました"));
    }
}