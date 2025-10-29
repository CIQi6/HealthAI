package com.example.healthai.prompt.domain;

import com.example.healthai.common.model.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate extends BaseEntity {

    private Long id;
    private String code;
    private PromptChannel channel;
    private String modelName;
    private String language;
    private String version;
    private String description;
    private String content;
    private String variables;
    private boolean enabled;

    public boolean isActive() {
        return enabled;
    }
}
