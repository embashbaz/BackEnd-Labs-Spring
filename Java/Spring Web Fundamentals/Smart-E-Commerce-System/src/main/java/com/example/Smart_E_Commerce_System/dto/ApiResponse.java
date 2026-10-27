package com.example.Smart_E_Commerce_System.dto;

/**
 * Standard envelope for every REST response: status, message, data.
 */
public record ApiResponse<T>(String status, String message, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>("error", message, data);
    }
}
