package com.example.healthai.consult.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConsultationReviewRequest(
        @NotBlank(message = "医生复核意见不能为空")
        @Size(max = 4000, message = "医生复核意见长度不能超过 4000 字符")
        String doctorOpinion,
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "^(AI_REVIEWED|DOCTOR_REVIEWED|CLOSED|REJECTED)$", message = "状态取值无效")
        String status
) {
}
