package com.example.healthai.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.healthai.auth.domain.User;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.auth.dto.AuthResponse;
import com.example.healthai.auth.dto.LoginRequest;
import com.example.healthai.auth.dto.RefreshTokenRequest;
import com.example.healthai.auth.dto.RegisterRequest;
import com.example.healthai.auth.dto.UserProfileResponse;
import com.example.healthai.auth.mapper.UserMapper;
import com.example.healthai.auth.security.JwtProvider;
import com.example.healthai.audit.AuditConstants;
import com.example.healthai.audit.service.AuditTrailService;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.profile.domain.HealthProfile;
import com.example.healthai.profile.mapper.HealthProfileMapper;

@Service
public class AuthService {

	private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final HealthProfileMapper healthProfileMapper;
    private final AuditTrailService auditTrailService;

    public AuthService(UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtProvider jwtProvider,
            RefreshTokenService refreshTokenService,
            HealthProfileMapper healthProfileMapper,
            AuditTrailService auditTrailService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.healthProfileMapper = healthProfileMapper;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        userMapper.findByUsername(request.username()).ifPresent(user -> {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        });

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
            .username(request.username())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .gender(request.gender() == null || request.gender().isBlank() ? "unknown" : request.gender())
            .phone(request.phone())
            .email(request.email())
            .userType(UserType.PATIENT)
            .registeredAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
        userMapper.insert(user);

        HealthProfile profile = HealthProfile.builder()
            .userId(user.getId())
            .createdAt(now)
            .updatedAt(now)
            .build();
        healthProfileMapper.insert(profile);
        auditTrailService.recordAuthEvent(
            AuditConstants.ACTION_AUTH_REGISTER,
            user.getId(),
            user.getUserType().name(),
            user.getId().toString(),
            "{\"username\":\"" + user.getUsername() + "\"}");
        return toProfileResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            String accessToken = jwtProvider.generateToken(authentication);
            long accessTokenExpiresInSeconds = jwtProvider.getExpirySeconds();
            RefreshTokenService.GeneratedRefreshToken refreshToken = refreshTokenService.create(request.username());

            userMapper.findByUsername(request.username()).ifPresent(user -> {
                LocalDateTime now = LocalDateTime.now();
                userMapper.updateLastLoginAt(user.getId(), now, now);
                auditTrailService.recordAuthEvent(
                    AuditConstants.ACTION_AUTH_LOGIN_SUCCESS,
                    user.getId(),
                    user.getUserType().name(),
                    user.getId().toString(),
                    "{\"username\":\"" + user.getUsername() + "\"}");
            });
            return new AuthResponse(
                accessToken,
                accessTokenExpiresInSeconds,
                refreshToken.token(),
                refreshToken.expiresInSeconds());
        } catch (AuthenticationException ex) {
            userMapper.findByUsername(request.username()).ifPresentOrElse(
                user -> auditTrailService.recordAuthEvent(
                    AuditConstants.ACTION_AUTH_LOGIN_FAILED,
                    user.getId(),
                    user.getUserType().name(),
                    user.getId().toString(),
                    "{\"username\":\"" + user.getUsername() + "\"}"),
                () -> auditTrailService.recordAuthEvent(
                    AuditConstants.ACTION_AUTH_LOGIN_FAILED,
                    null,
                    null,
                    request.username(),
                    "{\"username\":\"" + request.username() + "\"}"));
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String username;
        try {
            username = refreshTokenService.consume(request.refreshToken());
        } catch (BusinessException ex) {
            auditTrailService.recordAuthEvent(
                AuditConstants.ACTION_AUTH_REFRESH_FAILED,
                null,
                null,
                request.refreshToken(),
                "{\"errorCode\":\"" + ex.getErrorCode().getCode() + "\"}");
            throw ex;
        }
        String accessToken = jwtProvider.generateToken(username);
        long accessTokenExpiresInSeconds = jwtProvider.getExpirySeconds();
        RefreshTokenService.GeneratedRefreshToken newRefreshToken = refreshTokenService.create(username);

        userMapper.findByUsername(username).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            userMapper.updateLastLoginAt(user.getId(), now, now);
            auditTrailService.recordAuthEvent(
                AuditConstants.ACTION_AUTH_REFRESH_SUCCESS,
                user.getId(),
                user.getUserType().name(),
                user.getId().toString(),
                "{\"username\":\"" + user.getUsername() + "\"}" );
        });

        return new AuthResponse(
            accessToken,
            accessTokenExpiresInSeconds,
            newRefreshToken.token(),
            newRefreshToken.expiresInSeconds());
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        auditTrailService.recordAuthEvent(
            AuditConstants.ACTION_AUTH_LOGOUT,
            null,
            null,
            request.refreshToken(),
            "{\"refreshToken\":\"masked\"}");
    }

	@Transactional(readOnly = true)
	public UserProfileResponse currentProfile(String username) {
		return userMapper.findByUsername(username)
			.map(this::toProfileResponse)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
	}

	private UserProfileResponse toProfileResponse(User user) {
		return new UserProfileResponse(
			user.getId(),
			user.getUsername(),
			user.getFullName(),
			user.getGender() == null || user.getGender().isBlank() ? "unknown" : user.getGender(),
			user.getPhone(),
			user.getEmail(),
			user.getUserType(),
			user.getRegisteredAt(),
			user.getLastLoginAt());
	}

	public String healthProbe() {
		return "auth-service-ok";
	}

}
