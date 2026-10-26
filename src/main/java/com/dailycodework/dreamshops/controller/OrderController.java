package com.dailycodework.dreamshops.controller;

import com.dailycodework.dreamshops.response.ApiResponse;
import com.dailycodework.dreamshops.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import static com.dailycodework.dreamshops.security.TokenService.userId;

@RestController
@RequestMapping("${api.prefix}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** Check out: place an order for everything in the cart. */
    @PostMapping
    public ResponseEntity<ApiResponse> placeOrder(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Order placed", orderService.placeOrder(userId(jwt))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getOrders(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new ApiResponse("Your orders", orderService.getOrders(userId(jwt))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("Order found", orderService.getOrder(userId(jwt), id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("Order cancelled", orderService.cancelOrder(userId(jwt), id)));
    }
}
