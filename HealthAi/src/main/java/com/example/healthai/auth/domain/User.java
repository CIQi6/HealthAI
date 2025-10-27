package com.example.healthai.auth.domain;

import com.example.healthai.common.model.BaseEntity;
import lombok.Data;

@Data
public class User extends BaseEntity {

    private Long id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String gender;
    private String phone;
    private String email;
    private String userType;
}
