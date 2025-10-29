package com.example.healthai.audit;

public final class AuditConstants {

    private AuditConstants() {
    }

    public static final String RESOURCE_AUTH = "AUTH";
    public static final String RESOURCE_HEALTH_PROFILE = "HEALTH_PROFILE";
    public static final String RESOURCE_CONSULTATION = "CONSULTATION";

    public static final String ACTION_AUTH_REGISTER = "AUTH_REGISTER_SUCCESS";
    public static final String ACTION_AUTH_LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
    public static final String ACTION_AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
    public static final String ACTION_AUTH_REFRESH_SUCCESS = "AUTH_REFRESH_SUCCESS";
    public static final String ACTION_AUTH_REFRESH_FAILED = "AUTH_REFRESH_FAILED";
    public static final String ACTION_AUTH_LOGOUT = "AUTH_LOGOUT";

    public static final String ACTION_PROFILE_UPSERT = "PROFILE_UPSERT";
    public static final String ACTION_PROFILE_DELETE = "PROFILE_DELETE";

    public static final String ACTION_CONSULT_CREATED = "CONSULT_CREATED";
    public static final String ACTION_CONSULT_AI_COMPLETED = "CONSULT_AI_COMPLETED";
    public static final String ACTION_CONSULT_REVIEWED = "CONSULT_REVIEWED";
    public static final String ACTION_CONSULT_CLOSED = "CONSULT_CLOSED";
}
