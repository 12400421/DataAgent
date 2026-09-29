package com.jiayi.dataagent.exception;

import org.springframework.http.HttpStatus;

public class DatasetException extends RuntimeException {

    private final HttpStatus status;

    public DatasetException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public DatasetException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    public DatasetException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public DatasetException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
