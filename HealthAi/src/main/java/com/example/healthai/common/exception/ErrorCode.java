package com.example.healthai.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED("VAL-001", HttpStatus.BAD_REQUEST, "请求参数校验失败"),
    RESOURCE_NOT_FOUND("RES-404", HttpStatus.NOT_FOUND, "资源不存在"),
    BUSINESS_RULE_VIOLATION("BUS-409", HttpStatus.CONFLICT, "业务规则冲突"),
    INTERNAL_ERROR("SYS-500", HttpStatus.INTERNAL_SERVER_ERROR, "系统异常"),
    UNAUTHORIZED("AUTH-401", HttpStatus.UNAUTHORIZED, "未授权访问"),
    FORBIDDEN("AUTH-403", HttpStatus.FORBIDDEN, "权限不足");

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
