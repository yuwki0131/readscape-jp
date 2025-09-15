package jp.readscape.consumer.controllers.carts;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.readscape.consumer.dto.ApiResponse;
import jp.readscape.consumer.dto.carts.AddToCartRequest;
import jp.readscape.consumer.dto.carts.CartResponse;
import jp.readscape.consumer.dto.carts.UpdateCartQuantityRequest;
import jp.readscape.consumer.services.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartsController.class)
class CartsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "CONSUMER")
    void getCart_WithAuthenticatedUser_ShouldReturnCartResponse() throws Exception {
        // Given
        CartResponse mockResponse = CartResponse.builder()
                .cartId(1L)
                .items(Collections.emptyList())
                .totalAmount(BigDecimal.ZERO)
                .totalItemCount(0)
                .build();

        when(cartService.getCartByUsername("user")).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/cart")
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.itemCount").value(0));

        verify(cartService).getCartByUsername("user");
    }

    @Test
    void getCart_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).getCartByUsername(any());
    }

    @Test
    @WithMockUser(roles = "USER") // Different role
    void getCart_WithIncorrectRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isForbidden());

        verify(cartService, never()).getCartByUsername(any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addToCart_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(1L)
                .quantity(2)
                .build();

        doNothing().when(cartService).addToCart("user", 1L, 2);

        // When & Then
        mockMvc.perform(post("/api/cart")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("商品をカートに追加しました"));

        verify(cartService).addToCart("user", 1L, 2);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addToCart_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - missing required fields
        AddToCartRequest request = AddToCartRequest.builder()
                .build(); // Missing bookId and quantity

        // When & Then
        mockMvc.perform(post("/api/cart")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addToCart_WithNegativeQuantity_ShouldReturnBadRequest() throws Exception {
        // Given
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(1L)
                .quantity(-1) // Invalid quantity
                .build();

        // When & Then
        mockMvc.perform(post("/api/cart")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addToCart_WithZeroQuantity_ShouldReturnBadRequest() throws Exception {
        // Given
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(1L)
                .quantity(0) // Invalid quantity
                .build();

        // When & Then
        mockMvc.perform(post("/api/cart")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(any(), any(), any());
    }

    @Test
    void addToCart_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Given
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(1L)
                .quantity(2)
                .build();

        // When & Then
        mockMvc.perform(post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).addToCart(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void updateCartQuantity_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        UpdateCartQuantityRequest request = UpdateCartQuantityRequest.builder()
                .quantity(3)
                .build();

        doNothing().when(cartService).updateCartQuantity("user", 1L, 3);

        // When & Then
        mockMvc.perform(put("/api/cart/1")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("カート内商品の数量を変更しました"));

        verify(cartService).updateCartQuantity("user", 1L, 3);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void updateCartQuantity_WithInvalidQuantity_ShouldReturnBadRequest() throws Exception {
        // Given
        UpdateCartQuantityRequest request = UpdateCartQuantityRequest.builder()
                .quantity(-1) // Invalid quantity
                .build();

        // When & Then
        mockMvc.perform(put("/api/cart/1")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).updateCartQuantity(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void updateCartQuantity_WithZeroQuantity_ShouldCallRemoveFromCart() throws Exception {
        // Given
        UpdateCartQuantityRequest request = UpdateCartQuantityRequest.builder()
                .quantity(0) // This should effectively remove the item
                .build();

        doNothing().when(cartService).updateCartQuantity("user", 1L, 0);

        // When & Then
        mockMvc.perform(put("/api/cart/1")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cartService).updateCartQuantity("user", 1L, 0);
    }

    @Test
    void updateCartQuantity_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Given
        UpdateCartQuantityRequest request = UpdateCartQuantityRequest.builder()
                .quantity(3)
                .build();

        // When & Then
        mockMvc.perform(put("/api/cart/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).updateCartQuantity(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void removeFromCart_WithValidBookId_ShouldReturnSuccess() throws Exception {
        // Given
        doNothing().when(cartService).removeFromCart("user", 1L);

        // When & Then
        mockMvc.perform(delete("/api/cart/1")
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("商品をカートから削除しました"));

        verify(cartService).removeFromCart("user", 1L);
    }

    @Test
    void removeFromCart_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/cart/1"))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).removeFromCart(any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void removeFromCart_WithServiceException_ShouldPropagateException() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Book not found in cart"))
                .when(cartService).removeFromCart("user", 1L);

        // When & Then
        mockMvc.perform(delete("/api/cart/1")
                .with(user("user")))
                .andExpect(status().isBadRequest()); // Assuming exception handler converts to 400

        verify(cartService).removeFromCart("user", 1L);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void clearCart_WithAuthenticatedUser_ShouldReturnSuccess() throws Exception {
        // Given
        doNothing().when(cartService).clearCart("user");

        // When & Then
        mockMvc.perform(delete("/api/cart")
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("カートを空にしました"));

        verify(cartService).clearCart("user");
    }

    @Test
    void clearCart_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).clearCart(any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void clearCart_WithServiceException_ShouldPropagateException() throws Exception {
        // Given
        doThrow(new RuntimeException("Cart clearing failed"))
                .when(cartService).clearCart("user");

        // When & Then
        mockMvc.perform(delete("/api/cart")
                .with(user("user")))
                .andExpect(status().isInternalServerError()); // Assuming exception handler converts to 500

        verify(cartService).clearCart("user");
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addToCart_WithValidLargeQuantity_ShouldReturnSuccess() throws Exception {
        // Given
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(1L)
                .quantity(99) // Large but valid quantity
                .build();

        doNothing().when(cartService).addToCart("user", 1L, 99);

        // When & Then
        mockMvc.perform(post("/api/cart")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cartService).addToCart("user", 1L, 99);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void addToCart_WithEmptyRequestBody_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/cart")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")) // Empty request body
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void updateCartQuantity_WithNonNumericBookId_ShouldReturnBadRequest() throws Exception {
        // Given
        UpdateCartQuantityRequest request = UpdateCartQuantityRequest.builder()
                .quantity(3)
                .build();

        // When & Then
        mockMvc.perform(put("/api/cart/invalid")
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).updateCartQuantity(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void removeFromCart_WithNonNumericBookId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/cart/invalid")
                .with(user("user")))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).removeFromCart(any(), any());
    }
}