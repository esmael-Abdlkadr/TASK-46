package com.eaglepoint.workforce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private int status;
    private String code;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiError() {}
    public ApiError(int status, String code, String message) {
        this.status = status; this.code = code; this.message = message;
    }
    public ApiError(int status, String code, String message, List<String> details) {
        this(status, code, message); this.details = details;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
