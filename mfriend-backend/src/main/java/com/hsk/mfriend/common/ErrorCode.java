package com.hsk.mfriend.common;

/**
 * Date:2024/05/08
 * Author:hsk
 */
public enum ErrorCode {

    PARAMS_ERROR(40000,"请求参数错误",""),
    PARAMS_NULL(40001,"请求参数为空",""),
    NO_LOGIN(40100,"未登录",""),
    NO_AUTH(40101,"没有权限",""),
    SYSTEM_ERROR(50000,"系统内部异常","");

    private final int code;

    /**
     * 状态码信息
     */
    private final String message;

    /**
     * 状态码信息（详情）
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
