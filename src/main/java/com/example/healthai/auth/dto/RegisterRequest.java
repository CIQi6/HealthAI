package com.example.healthai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 64, message = "用户名长度需在4-64字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在8-64字符之间")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "姓名长度不能超过100个字符")
        String fullName,

        @Pattern(regexp = "^(male|female|unknown)$", message = "性别取值应为male/female/unknown")
        String gender,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Email(message = "邮箱格式不正确")
        String email
) {
}
