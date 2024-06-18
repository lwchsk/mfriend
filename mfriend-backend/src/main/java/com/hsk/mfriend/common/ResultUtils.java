package com.hsk.mfriend.common;

/**
 * Date:2024/05/08
 * Author:hsk
 */
public class ResultUtils {
    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<T>(0, data, "ok", "");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @param <T>
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<T>(errorCode);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode, String description) {
        return new BaseResponse<T>(errorCode,description);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode,String message, String description) {
        return new BaseResponse<T>(errorCode,description);
    }

    public static <T> BaseResponse<T> error(int code, String description) {
        return new BaseResponse<T>(code,description);
    }
}
