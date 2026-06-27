package doody.spring.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
    int status,
    @JsonProperty("isSuccess")
    boolean success,
    String message,
    T result
) {

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T result) {
        return new ApiResponse<>(status.value(), true, message, result);
    }

    public static <T> ApiResponse<T> fail(HttpStatus status, String message, T result) {
        return new ApiResponse<>(status.value(), false, message, result);
    }
}