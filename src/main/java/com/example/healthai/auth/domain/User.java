package com.example.healthai.auth.domain;

import java.time.LocalDateTime;

import com.example.healthai.common.model.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    private Long id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String gender;
    private String phone;
    private String email;
    private UserType userType;
    private LocalDateTime registeredAt;
    private LocalDateTime lastLoginAt;
}
