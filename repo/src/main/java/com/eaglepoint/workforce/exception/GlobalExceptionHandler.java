package com.eaglepoint.workforce.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied() {
        return "error/403";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound() {
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral() {
        return "error/500";
    }
}
