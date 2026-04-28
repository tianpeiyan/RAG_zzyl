package com.zzyl.assistant.exception;

import lombok.Getter;

@Getter
public class AssistantServiceException extends RuntimeException {

    private final int code;

    public AssistantServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AssistantServiceException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
