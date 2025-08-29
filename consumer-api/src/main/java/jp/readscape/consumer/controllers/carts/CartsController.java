package jp.readscape.consumer.controllers.carts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jp.readscape.consumer.dto.ApiResponse;
import jp.readscape.consumer.dto.carts.AddToCartRequest;
import jp.readscape.consumer.dto.carts.CartResponse;
import jp.readscape.consumer.dto.carts.UpdateCartQuantityRequest;
import jp.readscape.consumer.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "ショッピングカート管理API")
public class CartsController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "カート内容取得", description = "認証済みユーザーのカート内容を取得します")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<CartResponse> getCart(Authentication auth) {
        String username = auth.getName();
        CartResponse cartResponse = cartService.getCartByUsername(username);
        return ResponseEntity.ok(cartResponse);
    }

    @PostMapping
    @Operation(summary = "カートに商品を追加", description = "指定された書籍をカートに追加します")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication auth
    ) {
        String username = auth.getName();
        cartService.addToCart(username, request.getBookId(), request.getQuantity());
        
        return ResponseEntity.ok(ApiResponse.success("商品をカートに追加しました"));
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "カート内商品の数量を変更", description = "カート内の指定された書籍の数量を変更します")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> updateCartQuantity(
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateCartQuantityRequest request,
            Authentication auth
    ) {
        String username = auth.getName();
        cartService.updateCartQuantity(username, bookId, request.getQuantity());
        
        return ResponseEntity.ok(ApiResponse.success("カート内商品の数量を変更しました"));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "カートから商品を削除", description = "カートから指定された書籍を削除します")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> removeFromCart(
            @PathVariable Long bookId,
            Authentication auth
    ) {
        String username = auth.getName();
        cartService.removeFromCart(username, bookId);
        
        return ResponseEntity.ok(ApiResponse.success("商品をカートから削除しました"));
    }

    @DeleteMapping
    @Operation(summary = "カートを空にする", description = "カート内のすべての商品を削除します")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse> clearCart(Authentication auth) {
        String username = auth.getName();
        cartService.clearCart(username);
        
        return ResponseEntity.ok(ApiResponse.success("カートを空にしました"));
    }
}