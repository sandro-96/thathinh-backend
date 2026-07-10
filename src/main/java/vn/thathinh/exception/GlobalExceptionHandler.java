package vn.thathinh.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.ApiResponseDto;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation failed {} {}: {}", request.getMethod(), request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(ApiResponseDto.error(ApiCode.VALIDATION_ERROR, errors));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Business error {} {} [{}]: {}", request.getMethod(), request.getRequestURI(),
                    ex.getApiCode().getCode(), ex.getMessage());
        } else {
            log.debug("Business error {} {} [{}]: {}", request.getMethod(), request.getRequestURI(),
                    ex.getApiCode().getCode(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponseDto.error(ex.getApiCode()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Not found {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDto.error(ex.getApiCode()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAccessDenied(HttpServletRequest request) {
        log.warn("Access denied {} {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponseDto.error(ApiCode.ACCESS_DENIED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDto.error(ApiCode.INTERNAL_ERROR));
    }
}
