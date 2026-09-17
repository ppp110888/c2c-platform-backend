package com.c2c.order.exception;

import com.c2c.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 专门拦截 RuntimeException 异常，把它包装成咱们统一的 Result 格式返回给前端
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        // 之前 IDEA 提示过，咱们的 Result 支持传 (状态码, 错误信息) 两个参数
        return Result.failed(500, e.getMessage());
    }
}