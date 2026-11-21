package com.aarondevs.luminis.Parameters.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private String validation;
    private int status;
    private T data;
    private String mensage;

    public static <T> ApiResponse<T> success(int status, T data, String mensage) {
        return new ApiResponse<>("Success", status, data, mensage);
    }

    public static <T> ApiResponse<T> error(int status, String mensage) {
        return new ApiResponse<>("Error", status, null, mensage);
    }

    public static <T> ApiResponse<T> warning(int status, T data, String mensage) {
        return new ApiResponse<>("Warning", status, data, mensage);
    }

    public static <T> ApiResponse<T> information(int status, T data, String mensage) {
        return new ApiResponse<>("Information", status, data, mensage);
    }
}
