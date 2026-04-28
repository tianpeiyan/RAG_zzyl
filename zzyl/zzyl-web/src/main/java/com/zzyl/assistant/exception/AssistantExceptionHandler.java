package com.zzyl.assistant.exception;

import com.zzyl.base.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AssistantExceptionHandler {

    @ExceptionHandler(AssistantServiceException.class)
    public ResponseResult<Object> handleAssistantServiceException(AssistantServiceException exception) {
        log.warn("AI assistant request failed: {}", exception.getMessage());
        return ResponseResult.error(exception.getCode(), exception.getMessage());
    }
}
