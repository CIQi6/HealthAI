package com.example.healthai.profile.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.healthai.auth.mapper.UserMapper;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.profile.domain.HealthProfile;
import com.example.healthai.profile.dto.HealthProfileRequest;
import com.example.healthai.profile.dto.HealthProfileResponse;
import com.example.healthai.profile.mapper.HealthProfileMapper;
import com.example.healthai.audit.AuditConstants;
import com.example.healthai.audit.service.AuditTrailService;

@Service
public class HealthProfileService {

    private final HealthProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final AuditTrailService auditTrailService;

    public HealthProfileService(HealthProfileMapper profileMapper, UserMapper userMapper, AuditTrailService auditTrailService) {
        this.profileMapper = profileMapper;
        this.userMapper = userMapper;
        this.auditTrailService = auditTrailService;
    }

    public String healthProbe() {
        return "health-profile-service-ok";
    }

    @Transactional(readOnly = true)
    public HealthProfileResponse findByUsername(String username) {
        Long userId = userMapper.findByUsername(username)
            .map(com.example.healthai.auth.domain.User::getId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "未找到用户会话"));
        return profileMapper.findByUserId(userId)
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康档案不存在"));
    }

    @Transactional
    public HealthProfileResponse createOrUpdate(String username, HealthProfileRequest request) {
        var user = userMapper.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "未找到用户会话"));

        var existing = profileMapper.findByUserId(user.getId());
        HealthProfile profile = existing.orElseGet(() -> HealthProfile.builder().userId(user.getId()).build());
        applyRequest(profile, request);
        profile.setUserId(user.getId());

        LocalDateTime now = LocalDateTime.now();
        if (profile.getId() == null) {
            profile.setCreatedAt(now);
        }
        profile.setUpdatedAt(now);

        if (existing.isPresent()) {
            profileMapper.update(profile);
        } else {
            profileMapper.insert(profile);
        }
        HealthProfile saved = profileMapper.findByUserId(user.getId()).orElseThrow();
        auditTrailService.recordHealthProfileEvent(
            AuditConstants.ACTION_PROFILE_UPSERT,
            user.getId(),
            user.getUserType().name(),
            saved.getId() != null ? saved.getId().toString() : null,
            null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public HealthProfileResponse findById(String username, Long id) {
        var user = userMapper.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "未找到用户会话"));
        return profileMapper.findById(id)
            .filter(profile -> profile.getUserId().equals(user.getId()))
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康档案不存在"));
    }

    @Transactional
    public void delete(String username, Long id) {
        var user = userMapper.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "未找到用户会话"));
        var profile = profileMapper.findById(id)
            .filter(p -> p.getUserId().equals(user.getId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康档案不存在"));
        profileMapper.delete(profile.getId());
        auditTrailService.recordHealthProfileEvent(
            AuditConstants.ACTION_PROFILE_DELETE,
            user.getId(),
            user.getUserType().name(),
            profile.getId().toString(),
            null);
    }

    private HealthProfileResponse toResponse(HealthProfile profile) {
        return new HealthProfileResponse(
            profile.getId(),
            profile.getUserId(),
            profile.getBirthDate(),
            profile.getBloodType(),
            profile.getChronicDiseases(),
            profile.getAllergyHistory(),
            profile.getGeneticRisk()
        );
    }

    private void applyRequest(HealthProfile profile, HealthProfileRequest request) {
        profile.setBirthDate(request.birthDate());
        profile.setBloodType(request.bloodType());
        profile.setChronicDiseases(request.chronicDiseases());
        profile.setAllergyHistory(request.allergyHistory());
        profile.setGeneticRisk(request.geneticRisk());
    }
}
