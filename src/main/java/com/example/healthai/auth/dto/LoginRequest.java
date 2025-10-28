package com.example.healthai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 64, message = "用户名长度需在4-64字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在8-64字符之间")
        String password
) {
}
