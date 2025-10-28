package com.example.healthai.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.healthai.auth.dto.AuthResponse;
import com.example.healthai.auth.dto.LoginRequest;
import com.example.healthai.auth.dto.RefreshTokenRequest;
import com.example.healthai.auth.dto.RegisterRequest;
import com.example.healthai.auth.dto.UserProfileResponse;
import com.example.healthai.auth.service.AuthService;
import com.example.healthai.common.api.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/probe")
	public ApiResponse<String> probe() {
		return ApiResponse.success(authService.healthProbe());
	}

	@PostMapping("/register")
	public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.success(authService.register(request));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ApiResponse.success(null);
	}

	@GetMapping("/profile")
	public ApiResponse<UserProfileResponse> currentProfile(Authentication authentication) {
		return ApiResponse.success(authService.currentProfile(authentication.getName()));
	}
}
