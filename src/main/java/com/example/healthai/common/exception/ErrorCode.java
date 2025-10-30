package com.example.healthai.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED("VAL-001", HttpStatus.BAD_REQUEST, "请求参数校验失败"),
    RESOURCE_NOT_FOUND("RES-404", HttpStatus.NOT_FOUND, "资源不存在"),
    BUSINESS_RULE_VIOLATION("BUS-409", HttpStatus.CONFLICT, "业务规则冲突"),
    INTERNAL_ERROR("SYS-500", HttpStatus.INTERNAL_SERVER_ERROR, "系统异常"),
    UNAUTHORIZED("AUTH-401", HttpStatus.UNAUTHORIZED, "未授权访问"),
    FORBIDDEN("AUTH-403", HttpStatus.FORBIDDEN, "权限不足"),
    USERNAME_ALREADY_EXISTS("AUTH-409", HttpStatus.CONFLICT, "用户名已存在"),
    INVALID_CREDENTIALS("AUTH-400", HttpStatus.BAD_REQUEST, "账号或密码错误"),
    REFRESH_TOKEN_INVALID("AUTH-410", HttpStatus.UNAUTHORIZED, "刷新令牌无效"),
    REFRESH_TOKEN_EXPIRED("AUTH-411", HttpStatus.UNAUTHORIZED, "刷新令牌已过期"),
    PROMPT_TEMPLATE_NOT_FOUND("PROMPT-404", HttpStatus.NOT_FOUND, "提示词模板不存在"),
    LLM_CALL_FAILED("LLM-500", HttpStatus.BAD_GATEWAY, "大模型调用失败"),
    LLM_TIMEOUT("LLM-504", HttpStatus.GATEWAY_TIMEOUT, "大模型响应超时"),
    PROMPT_RENDER_FAILED("PROMPT-500", HttpStatus.INTERNAL_SERVER_ERROR, "提示词渲染失败"),
    CONSULTATION_NOT_FOUND("CONSULT-404", HttpStatus.NOT_FOUND, "问诊记录不存在"),
    CONSULTATION_FORBIDDEN("CONSULT-403", HttpStatus.FORBIDDEN, "无权访问问诊记录"),
    CONSULTATION_STATUS_CONFLICT("CONSULT-409", HttpStatus.CONFLICT, "问诊状态不允许当前操作"),
    DRUG_NOT_FOUND("DRUG-404", HttpStatus.NOT_FOUND, "药品不存在"),
    DRUG_GENERIC_NAME_CONFLICT("DRUG-409", HttpStatus.CONFLICT, "药品通用名已存在"),
    DRUG_CONTRAINDICATED("DRUG-412", HttpStatus.PRECONDITION_FAILED, "药品存在禁忌，无法继续"),
    PRESCRIPTION_NOT_FOUND("RX-404", HttpStatus.NOT_FOUND, "处方不存在"),
    PRESCRIPTION_STATUS_CONFLICT("RX-409", HttpStatus.CONFLICT, "处方状态不允许当前操作");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
