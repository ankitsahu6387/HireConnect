package com.hireconnect.analyticsservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.analyticsservice.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AnalyticsException.class)
    public ResponseEntity<ApiResponse> handleAnalyticsException(AnalyticsException ex) {

        ApiResponse response = new ApiResponse(ex.getMessage(), false, null);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {

        ApiResponse response = new ApiResponse(ex.getMessage(), false, null);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}