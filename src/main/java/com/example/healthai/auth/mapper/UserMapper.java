package com.example.healthai.auth.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.auth.domain.User;

@Mapper
public interface UserMapper {
    Optional<User> findByUsername(@Param("username") String username);

    int insert(User user);

    int updateLastLoginAt(@Param("id") Long id,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt,
                          @Param("updatedAt") LocalDateTime updatedAt);
}
