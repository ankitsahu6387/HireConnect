package com.hireconnect.interviewservice.exception;

public class InvalidInterviewStatusException extends RuntimeException {

    public InvalidInterviewStatusException(String message) {
        super(message);
    }
}