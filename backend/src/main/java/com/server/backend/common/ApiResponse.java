package com.server.backend.common;

public record ApiResponse<T>(boolean success, T data, String msg) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "ok");
    }

    public static <T> ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(false, null, msg);
    }
}
