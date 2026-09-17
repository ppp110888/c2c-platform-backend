package com.c2c.user.exception;

import com.c2c.common.exception.BusinessException;
import com.c2c.common.result.Result;
import com.c2c.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 拦截全局 Controller 抛出的异常
public class GlobalExceptionHandler {

    // 1. 拦截已知的核心业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("=== 业务异常捕获: {} ===", e.getMessage());
        return Result.failed(e.getResultCode());
    }

    // 2. 拦截 JSR303 参数校验异常 (后续前端传参校验会用到)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        String defaultMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        log.error("=== 参数校验异常捕获: {} ===", defaultMessage);
        return Result.failed(ResultCode.VALIDATE_FAILED.getCode(), defaultMessage);
    }

    // 3. 拦截未知的运行时异常（系统崩溃、空指针等）
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("=== 系统未知异常捕获 ===", e);
        return Result.failed(ResultCode.BUSINESS_ERROR.getCode(), "系统开小差了，请稍后再试");
    }
}