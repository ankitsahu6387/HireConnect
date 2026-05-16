package com.hireconnect.subscriptionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.subscriptionservice.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<ApiResponse> handleSubscriptionException(SubscriptionException ex) {

        return new ResponseEntity<>(
                new ApiResponse(ex.getMessage(), false),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {

        return new ResponseEntity<>(
                new ApiResponse("Something went wrong", false),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}