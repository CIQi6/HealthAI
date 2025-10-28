package com.example.healthai.auth.service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.healthai.auth.domain.UserType;
import com.example.healthai.auth.mapper.UserMapper;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userMapper.findByUsername(username)
                .map(user -> new User(
                        user.getUsername(),
                        user.getPasswordHash(),
                        Collections.singleton(new SimpleGrantedAuthority(roleName(user.getUserType())))))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private String roleName(UserType userType) {
        return "ROLE_" + userType.name();
    }
}
