package com.dailycodework.dreamshops.controller;

import com.dailycodework.dreamshops.request.CartItemRequest;
import com.dailycodework.dreamshops.response.ApiResponse;
import com.dailycodework.dreamshops.service.cart.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import static com.dailycodework.dreamshops.security.TokenService.userId;

/** The caller's own cart. There is no way to name another user's cart. */
@RestController
@RequestMapping("${api.prefix}/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new ApiResponse("Your cart", cartService.getCart(userId(jwt))));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse> addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartItemRequest request) {
        if (request.productId() == null) {
            throw new IllegalArgumentException("productId is required");
        }
        return ResponseEntity.ok(new ApiResponse("Added to cart", cartService.addItem(userId(jwt), request.productId(), request.quantity())));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse> setQuantity(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
                                                   @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(new ApiResponse("Cart updated", cartService.setQuantity(userId(jwt), productId, request.quantity())));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        return ResponseEntity.ok(new ApiResponse("Removed from cart", cartService.removeItem(userId(jwt), productId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> clear(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new ApiResponse("Cart emptied", cartService.clear(userId(jwt))));
    }
}
