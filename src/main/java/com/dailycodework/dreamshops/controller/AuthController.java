package com.dailycodework.dreamshops.controller;

import com.dailycodework.dreamshops.request.LoginRequest;
import com.dailycodework.dreamshops.request.RegisterRequest;
import com.dailycodework.dreamshops.response.ApiResponse;
import com.dailycodework.dreamshops.security.TokenService;
import com.dailycodework.dreamshops.service.user.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Account created", authService.register(request)));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new ApiResponse("Logged in", authService.login(request)));
    }

    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new ApiResponse("Current user", authService.getUser(TokenService.userId(jwt))));
    }
}
