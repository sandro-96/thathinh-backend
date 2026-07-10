package vn.thathinh.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.thathinh.constant.ApiCode;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private String timestamp;

    public static <T> ApiResponseDto<T> success(ApiCode code, T data) {
        return new ApiResponseDto<>(true, code.getCode(), code.getMessage(), data, Instant.now().toString());
    }

    public static <T> ApiResponseDto<T> success(ApiCode code) {
        return success(code, null);
    }

    public static <T> ApiResponseDto<T> error(ApiCode code) {
        return new ApiResponseDto<>(false, code.getCode(), code.getMessage(), null, Instant.now().toString());
    }

    public static <T> ApiResponseDto<T> error(ApiCode code, T data) {
        return new ApiResponseDto<>(false, code.getCode(), code.getMessage(), data, Instant.now().toString());
    }
}
