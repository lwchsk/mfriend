package com.hsk.mfriend.exception;

import com.hsk.mfriend.common.ErrorCode;

/**
 * Date:2024/05/08
 * Author:hsk
 */
public class BusinessException extends RuntimeException{
    private final int code;
    private final String description;

    public BusinessException(int code, String description,String message) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode,String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
