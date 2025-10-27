package com.example.healthai.common.api;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null, Instant.now());
    }

    public static ApiResponse<Void> failure(String message) {
        return new ApiResponse<>(false, null, message, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>(false, data, message, Instant.now());
    }
}
