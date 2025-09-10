package jp.readscape.consumer.controllers.orders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.orders.CreateOrderRequest;
import jp.readscape.consumer.dto.orders.CreateOrderResponse;
import jp.readscape.consumer.dto.orders.OrderDetail;
import jp.readscape.consumer.dto.users.OrderSummary;
import jp.readscape.consumer.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "注文処理API")
public class OrdersController {

    private final OrderService orderService;

    @Operation(
        summary = "注文作成",
        description = "カート内容から注文を作成します。在庫チェックと減算を行い、トランザクション内で処理されます。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "注文作成成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラーまたはビジネスルールエラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "409", description = "在庫不足エラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth
    ) {
        log.info("POST /api/orders - user: {}", auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            CreateOrderResponse response = orderService.createOrderFromCart(user.getId(), request);
            
            log.info("Order created successfully: {} for user: {}", response.getOrderNumber(), user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Order creation failed for user {}: {}", auth.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Order creation failed due to business rule violation for user {}: {}", auth.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during order creation for user {}: {}", auth.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(jp.readscape.consumer.dto.ApiResponse.error("注文処理中にエラーが発生しました"));
        }
    }

    @Operation(
        summary = "注文履歴取得",
        description = "認証済みユーザーの注文履歴を取得します。新しい注文順で並びます。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注文履歴取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です")
    })
    @GetMapping
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummary>> getOrders(Authentication auth) {
        log.info("GET /api/orders - user: {}", auth.getName());

        User user = (User) auth.getPrincipal();
        List<OrderSummary> orders = orderService.getUserOrders(user.getId());
        
        return ResponseEntity.ok(orders);
    }

    @Operation(
        summary = "注文詳細取得",
        description = "指定された注文IDの詳細情報を取得します。本人の注文のみアクセス可能です。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注文詳細取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrderById(
            @Parameter(description = "注文ID", example = "1", required = true)
            @PathVariable Long id,
            Authentication auth
    ) {
        log.info("GET /api/orders/{} - user: {}", id, auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            OrderDetail orderDetail = orderService.getOrderDetail(id, user.getId());
            
            return ResponseEntity.ok(orderDetail);
        } catch (IllegalArgumentException e) {
            log.warn("Order not found or access denied: {} for user: {}", id, auth.getName());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "最近の注文取得",
        description = "認証済みユーザーの最近の注文を指定件数取得します。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "最近の注文取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です")
    })
    @GetMapping("/recent")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummary>> getRecentOrders(
            @Parameter(description = "取得件数", example = "5")
            @RequestParam(defaultValue = "5") Integer limit,
            Authentication auth
    ) {
        log.info("GET /api/orders/recent?limit={} - user: {}", limit, auth.getName());

        if (limit <= 0 || limit > 20) {
            limit = 5; // デフォルト値に戻す
        }

        User user = (User) auth.getPrincipal();
        List<OrderSummary> recentOrders = orderService.getRecentUserOrders(user.getId(), limit);
        
        return ResponseEntity.ok(recentOrders);
    }

    @Operation(
        summary = "注文キャンセル",
        description = "指定された注文をキャンセルします。キャンセル可能な状態の注文のみ処理できます。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注文キャンセル成功"),
        @ApiResponse(responseCode = "400", description = "キャンセル不可能な注文です",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません")
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> cancelOrder(
            @Parameter(description = "注文ID", example = "1", required = true)
            @PathVariable Long id,
            Authentication auth
    ) {
        log.info("POST /api/orders/{}/cancel - user: {}", id, auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            orderService.cancelOrder(id, user.getId());
            
            log.info("Order cancelled successfully: {} for user: {}", id, user.getUsername());
            return ResponseEntity.ok(jp.readscape.consumer.dto.ApiResponse.success("注文をキャンセルしました"));
            
        } catch (IllegalArgumentException e) {
            log.warn("Order not found for cancellation: {} for user: {}", id, auth.getName());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Order cancellation failed for order {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "注文統計取得",
        description = "認証済みユーザーの注文統計（注文回数、合計金額など）を取得します。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注文統計取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<OrderService.OrderStatistics> getOrderStatistics(Authentication auth) {
        log.info("GET /api/orders/statistics - user: {}", auth.getName());

        User user = (User) auth.getPrincipal();
        OrderService.OrderStatistics statistics = orderService.getOrderStatistics(user.getId());
        
        return ResponseEntity.ok(statistics);
    }
}