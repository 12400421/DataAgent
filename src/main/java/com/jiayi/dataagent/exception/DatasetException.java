package com.jiayi.dataagent.exception;

public class DatasetException extends RuntimeException {

    public DatasetException(String message) {
        super(message);
    }

    public DatasetException(String message, Throwable cause) {
        super(message, cause);
    }
}
