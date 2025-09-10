package jp.readscape.inventory.controllers.admin.orders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.readscape.inventory.dto.admin.AdminOrderDetail;
import jp.readscape.inventory.dto.admin.AdminOrderView;
import jp.readscape.inventory.dto.admin.PendingOrder;
import jp.readscape.inventory.dto.admin.UpdateOrderStatusRequest;
import jp.readscape.inventory.services.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin Orders", description = "管理者向け注文管理API")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminOrdersController {

    private final AdminOrderService adminOrderService;

    @Operation(
        summary = "注文一覧取得",
        description = "管理者向けの注文一覧を取得します。ステータスでフィルタリング可能です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注文一覧取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー", 
            content = @Content(schema = @Schema(implementation = jp.readscape.inventory.dto.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<AdminOrderView>> getOrders(
            @Parameter(description = "注文ステータス", example = "PENDING")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "ページ番号（0から開始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "ページサイズ", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "ソート条件", example = "orderDate")
            @RequestParam(defaultValue = "orderDate") String sortBy,
            
            @Parameter(description = "ソート順序", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("GET /admin/orders - status: {}, page: {}, size: {}", status, page, size);

        Page<AdminOrderView> orders = adminOrderService.getOrders(status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(orders);
    }

    @Operation(
        summary = "注文詳細取得",
        description = "指定されたIDの注文詳細情報を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注文詳細取得成功"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderDetail> getOrderById(
            @Parameter(description = "注文ID", example = "1", required = true)
            @PathVariable Long id
    ) {
        log.info("GET /admin/orders/{} - orderId: {}", id, id);

        AdminOrderDetail orderDetail = adminOrderService.getOrderById(id);
        return ResponseEntity.ok(orderDetail);
    }

    @Operation(
        summary = "注文ステータス更新",
        description = "指定された注文のステータスを更新します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ステータス更新成功"),
        @ApiResponse(responseCode = "400", description = "不正なステータス遷移"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<jp.readscape.inventory.dto.ApiResponse> updateOrderStatus(
            @Parameter(description = "注文ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        log.info("PUT /admin/orders/{}/status - orderId: {}, newStatus: {}", 
                id, id, request.getNewStatus());

        adminOrderService.updateOrderStatus(id, request);
        
        return ResponseEntity.ok(
            jp.readscape.inventory.dto.ApiResponse.success("注文ステータスを更新しました")
        );
    }

    @Operation(
        summary = "処理待ち注文取得",
        description = "処理が必要な注文（PENDING、CONFIRMED状態）を優先的に取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "処理待ち注文取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/pending")
    public ResponseEntity<List<PendingOrder>> getPendingOrders(
            @Parameter(description = "取得件数", example = "50")
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("GET /admin/orders/pending - limit: {}", limit);

        List<PendingOrder> pendingOrders = adminOrderService.getPendingOrders(limit);
        return ResponseEntity.ok(pendingOrders);
    }

    @Operation(
        summary = "注文検索",
        description = "顧客名、注文番号などで注文を検索します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "検索成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<AdminOrderView>> searchOrders(
            @Parameter(description = "検索キーワード", example = "ORD-2024")
            @RequestParam String query,
            
            @Parameter(description = "ページ番号", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "ページサイズ", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /admin/orders/search - query: {}, page: {}, size: {}", query, page, size);

        Page<AdminOrderView> searchResults = adminOrderService.searchOrders(query, page, size);
        return ResponseEntity.ok(searchResults);
    }

    @Operation(
        summary = "注文キャンセル",
        description = "指定された注文をキャンセルします"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "キャンセル成功"),
        @ApiResponse(responseCode = "400", description = "キャンセルできない状態"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<jp.readscape.inventory.dto.ApiResponse> cancelOrder(
            @Parameter(description = "注文ID", example = "1", required = true)
            @PathVariable Long id,
            
            @Parameter(description = "キャンセル理由", example = "顧客都合")
            @RequestParam(required = false) String reason
    ) {
        log.info("POST /admin/orders/{}/cancel - orderId: {}, reason: {}", id, id, reason);

        adminOrderService.cancelOrder(id, reason);
        
        return ResponseEntity.ok(
            jp.readscape.inventory.dto.ApiResponse.success("注文をキャンセルしました")
        );
    }

    @Operation(
        summary = "注文統計取得",
        description = "注文の統計情報を取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "統計取得成功"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/statistics")
    public ResponseEntity<AdminOrderService.OrderStatistics> getOrderStatistics(
            @Parameter(description = "統計期間（日数）", example = "30")
            @RequestParam(defaultValue = "30") int days
    ) {
        log.info("GET /admin/orders/statistics - days: {}", days);

        AdminOrderService.OrderStatistics statistics = adminOrderService.getOrderStatistics(days);
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "配送ラベル印刷用データ取得",
        description = "指定された注文の配送ラベル印刷用データを取得します"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "配送データ取得成功"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません"),
        @ApiResponse(responseCode = "403", description = "権限エラー")
    })
    @GetMapping("/{id}/shipping-label")
    public ResponseEntity<AdminOrderService.ShippingLabelData> getShippingLabelData(
            @Parameter(description = "注文ID", example = "1", required = true)
            @PathVariable Long id
    ) {
        log.info("GET /admin/orders/{}/shipping-label - orderId: {}", id, id);

        AdminOrderService.ShippingLabelData labelData = adminOrderService.getShippingLabelData(id);
        return ResponseEntity.ok(labelData);
    }
}