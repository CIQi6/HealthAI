package com.example.healthai.consult.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationCreateRequest(
        @NotBlank(message = "症状描述不能为空")
        @Size(max = 4000, message = "症状描述长度不能超过 4000 字符")
        String symptomDescription,
        @Size(max = 5, message = "附件列表最多 5 条")
        List<String> attachments,
        @Size(max = 128, message = "模板编号长度不能超过 128")
        String templateCode,
        Map<String, Object> variables
) {

    public ConsultationCreateRequest {
        attachments = attachments == null ? Collections.emptyList() : attachments;
        variables = variables == null ? Collections.emptyMap() : variables;
    }
}
