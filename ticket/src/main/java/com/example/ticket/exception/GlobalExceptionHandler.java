package com.example.ticket.exception;

import com.example.ticket.dto.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiMessageResponse> handleApiException(ApiException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(new ApiMessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessageResponse> handleValidationException(MethodArgumentNotValidException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiMessageResponse("Invalid request data"));
    }
}
